/*
 *  Copyright (c) 2011, Franz Bettag <franz@bett.ag>
 *  All rights reserved.
 *
 *  Redistribution and use in source and binary forms, with or without
 *  modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * All advertising materials mentioning features or use of this software
 *       must display the following acknowledgement:
 *       This product includes software developed by the Bettag Systems UG
 *       and its contributors.
 *
 *  THIS SOFTWARE IS PROVIDED BY BETTAG SYSTEMS UG ''AS IS'' AND ANY
 *  EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *  WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *  DISCLAIMED. IN NO EVENT SHALL BETTAG SYSTEMS UG BE LIABLE FOR ANY
 *  DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 *  (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 *  LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 *  ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 *  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 *  SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 */

package code.snippet

import net.liftweb.http._
import net.liftweb.http.js._
import net.liftweb.http.js.JE._
import net.liftweb.http.js.JsCmds._
import net.liftweb.http.SHtml._
import net.liftweb.util._
import net.liftweb.util.Helpers._
import net.liftweb.mapper._
import net.liftweb.common._
import net.liftweb.mongodb._
import net.liftweb.mongodb.record._
import net.liftweb.mongodb.record.field._
import net.liftweb.record._
import net.liftweb.record.field._

import scala.xml._
import scala.util.Random._
import java.util.{Date, Calendar, UUID}
import org.joda.time.{DateTime, DateTimeZone}
import com.foursquare.rogue.Rogue._
import org.bson.types._

import scalax.file.{Path, PathMatcher}
import scalax.file.PathMatcher._

import code.lib._
import code.model._


class Posts extends Loggable {

	private def postCssSel(p: Post) =
		".println_entry_link [href]" #>	p.link &
		".println_entry_link *" #>		p.name &
		".println_post_footer *" #>		DateTimeHelpers.postFooter(p) &
		".println_post_footer [id]" #>	"println_entry_footer_%s".format(p.id) &
		"#println_entry_body *" #>		p.contentText

	lazy val post: Box[Post] = Post.one(S.uri.replaceFirst("^/", ""))
	lazy val postCss: Box[CssSel] = post match {
		case Full(p: Post) => Full(postCssSel(p))
		case _ => Empty
	}
	
	def saveName(p: Post, n: String): JsCmd =
		p.name(n).saveWithJsFeedback(".println_post_name") &
		JsRaw("$('.println_entry_link').html('%s')".format(n.replaceAll("'", "\\'"))).cmd	
	
	def saveSlug(p: Post, n: String): JsCmd =
		if (p.slug(n).save && p.saved_?) 
			RedirectTo("/%s#open".format(n))
		else
			JsFx.failed(".println_post_slug") &
			JsRaw("$('.println_post_slug').val('%s')".format(p.slug)).cmd	

	def savePublishDate(p: Post, a: String): JsCmd =
		if (p.publishAt(a))
			JsFx.validated(".println_post_publish_date") &
			DateTimeHelpers.updateTimestamps(p.reload) &
			JsRaw("$('.println_post_slug').attr('disabled', 'disabled')")
		else
			JsFx.failed(".println_post_publish_date") &
			JsFx.invalidated(".println_post_publish_date")

	def setPublished(p: Post, b: Boolean): JsCmd =
		if (p.publish(b).saved_?)
			DateTimeHelpers.updateTimestamps(p.reload) &
			JsRaw("$('.println_post_slug').attr('disabled', 'disabled')")
		else
			JsFx.failed(".println_post_publish_now") &
			JsFx.invalidated(".println_post_publish_now") &
			JsRaw("$('.println_post_slug').removeAttr('disabled')")

	def saveTags(p: Post, t: String) = {
		println("---------------------------\n%s\n------------------------".format(t))
		Noop
	}

	/* snippets */
	def found(xhtml: NodeSeq) = if (post != Empty) postCss.open_!.apply(xhtml) else NodeSeq.Empty
	def notFound(xhtml: NodeSeq) = if (post == Empty) xhtml else NodeSeq.Empty

	def form: CssSel = post match {
		case Full(p: Post) => {
			
			// you need to directly pass _ to a method (in SHtml.ajax* below), otherwise you'll get errors.
			def setUnparsedContent(s: String): Post = p.contentCache(Unparsed(s).toString)
			def setUnparsedTeaser(s: String): Post = p.teaserCache(Unparsed(s).toString)

			val contentHandler = SHtml.ajaxTextarea(p.content, p.content(_).saveWithJsFeedback("#println-admin-txtc")) \\ "@onblur"
			val contentCacheHandler = {
				val h = SHtml.ajaxTextarea("", setUnparsedContent(_).saveWithNoop) \\ "@onblur"
				h.toString.replaceAll("this.value", "\\$('#println-admin-txtc-cached').val()")
			}
			
			val teaserHandler = SHtml.ajaxTextarea(p.teaser, p.teaser(_).saveWithJsFeedback("#println-admin-txtt")) \\ "@onblur"
			val teaserCacheHandler = {
				val h = SHtml.ajaxTextarea("", setUnparsedTeaser(_).saveWithNoop) \\ "@onblur"
				h.toString.replaceAll("this.value", "\\$('#println-admin-txtt-cached').val()")
			}


			".println_post_name" #>					SHtml.ajaxText(p.name, saveName(p, _)) &
			".println_post_tags" #>					SHtml.ajaxText("tags", saveTags(p, _)) &
			".println_post_teaser_link" #>			SHtml.ajaxText(p.teaserLink, p.teaserLink(_).saveWithJsFeedback(".println_post_teaser_link input")) &
			".println_post_publish_now" #>			SHtml.ajaxCheckbox(p.published, setPublished(p, _)) &
			".println_entry_delete" #>				a(() => p.deleteWithJsFeedback("article[id=post_%s]".format(p.id), p.name), <span>delete this Post</span>) &
			".println_post_publish_date" #>			SHtml.ajaxText(p.publishDate.toFormattedString, savePublishDate(p, _)) &
			"#println-admin-txtc" #>					<textarea onblur={"%s; %s".format(contentHandler, contentCacheHandler)}>{p.content}</textarea> &
			"#println-admin-txtt" #>					<textarea onblur={"%s; %s".format(teaserHandler, teaserCacheHandler)}>{p.teaser}</textarea> &
			".println_post_slug" #>					SHtml.ajaxText(p.slug, saveSlug(p, _), "disabled" -> { if (p.published) "disabled" else "" })
		}
			
		case _ => "#unlikely" #> "to happen"
	}

}
