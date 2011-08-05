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

	def filterURI(f: String) = f.replaceFirst("^/", "")

	lazy val post = Post.one(filterURI(if (S.uri.matches("^/ajax_request/")) S.referer.openOr("new-post") else S.uri)).open_!
	
	def saveName(p: Post, n: String): JsCmd =
		p.name(n).saveWithJsFeedback(".println_post_name") &
		JsRaw("$('.println_entry_link').html('%s')".format(n.replaceAll("'", "\\'"))).cmd	
	
	def saveSlug(p: Post, n: String): JsCmd =
		if (p.slug(HtmlHelpers.slugify(n)).validate.length == 0 && p.save) 
			RedirectTo("/%s".format(p.slug))
		else
			JsFx.failed(".println_post_slug") &
			JsFx.invalidated(".println_post_slug")
	//		JsRaw("$('.println_post_slug').val('%s')".format(p.reload.slug)).cmd

	def savePublishDate(p: Post, a: String): JsCmd =
		if (p.publishAt(a))
			JsFx.validated(".println_post_publish_date") &
			DateTimeHelpers.updateTimestamps(p.reload) &
			JsRaw("$('.println_post_slug').attr('disabled', 'disabled')")
		else
			JsFx.failed(".println_post_publish_date") &
			JsFx.invalidated(".println_post_publish_date")

	def setSlugEditing(enabled: Boolean): JsCmd =
		if (enabled)
			JsRaw("$('.println_post_slug').removeAttr('disabled')")
		else
			JsRaw("$('.println_post_slug').attr('disabled', 'disabled')")

	def setPublished(p: Post, b: Boolean): JsCmd =
		if (p.publish(b).saved_?)
			DateTimeHelpers.updateTimestamps(p.reload) &
			setSlugEditing(!b)
		else
			JsFx.failed(".println_post_publish_now") &
			JsFx.invalidated(".println_post_publish_now")

	def tags(p: Post): NodeSeq = {
		val addHandler = (SHtml.ajaxText("", addTag(p, _)) \\ "@onblur").toString.replaceAll("this.value", "item._value")
		val deleteHandler = (SHtml.ajaxText("", deleteTag(p, _)) \\ "@onblur").toString.replaceAll("this.value", "item._value")

		<xml:group>
			{Script(JsRaw("""
				println.tags.add = function(i) { var item = eval('(' + i + ')'); console.log(item); %s; };
				println.tags.delete = function(i) { var item = eval('(' + i + ')'); console.log(item); %s; };
			 """.format(addHandler, deleteHandler, "")))}
			<select name="println_post_tags" id="println_post_tags" class="println_post_tags" multiple="multiple">
				{Tag.findAll(OrderBy(Tag.name, Ascending), OrderBy(Tag.priority, Ascending)).map(tag => {
					val selected = if (PostTags.count(By(PostTags.post, p), By(PostTags.tag, tag)) != 0) "class=\"selected\"" else ""
					XML.loadString("<option value=\"%s\" %s>%s</option>".format(tag.id, selected, tag.name))
				})}
			</select>
		</xml:group>
	}

	def addTag(p: Post, t: String) = {
		val tag: Tag = try {
				val tid: Int = t.toInt
				Tag.find(By(Tag.id, tid)) match {
					case Full(tag: Tag) => tag
					case _ => throw new IllegalArgumentException("This is not an integer.")
				}
			} catch {
				case _ => Tag.find(By(Tag.name, t)) match {
					case Full(tag: Tag) => tag
					case _ => { val btag = Tag.create.name(t).slug(HtmlHelpers.slugify(t)); btag.save; btag }
				}
			}

		val pt = PostTags.create.post(p).tag(tag)
		println("--- Post %s (\"%s\") -- Added Tag: %s -- %s".format(p.id, p.name, t.name, pt.save))
		DateTimeHelpers.updateTimestamps(p.reload)
	}

	def deleteTag(p: Post, t: String): JsCmd = {
		var tag = Tag.create
	
		try {
			tag = Tag.find(By(Tag.id, t.toInt)) match {
				case Full(tag: Tag) => tag
				case _ => return Noop
			}
		}
		catch { case _ => {
			tag = Tag.find(By(Tag.name, t)) match {
				case Full(tag: Tag) => tag
				case _ => return Noop
			}
		}}
	
		val pt = PostTags.findAll(By(PostTags.post, p), By(PostTags.tag, tag))
		println("--- Post %s (\"%s\") -- Deleted Tag: %s -- %s".format(p.id, p.name, t.name, pt.map(pte => {PostTags.delete_!(pte)})))
		DateTimeHelpers.updateTimestamps(p.reload)
	}


	/* snippets */
	def title = <title>{HtmlHelpers.title(post.name)}</title>

	def found: CssSel =
		".println_entry_link [href]" #>	post.link &
		".println_entry_link *" #>		post.name &
		".println_post_footer *" #>		DateTimeHelpers.postFooter(post) &
		".println_post_footer [id]" #>	"println_entry_footer_%s".format(post.id) &
		"#println_entry_body *" #>		post.contentText

	def form: CssSel = {
		// you need to directly pass _ to a method (in SHtml.ajax* below), otherwise you'll get errors.
		def setUnparsedContent(s: String): Post = post.contentCache(Unparsed(s).toString)
		def setUnparsedTeaser(s: String): Post = post.teaserCache(Unparsed(s).toString)

		val contentHandler = SHtml.ajaxTextarea(post.content, post.content(_).saveWithJsFeedback("#println-admin-txtc")) \\ "@onblur"
		val contentCacheHandler = {
			val h = SHtml.ajaxTextarea("", setUnparsedContent(_).saveWithNoop) \\ "@onblur"
			h.toString.replaceAll("this.value", "\\$('#println-admin-txtc-cached').val()")
		}
		
		val teaserHandler = SHtml.ajaxTextarea(post.teaser, post.teaser(_).saveWithJsFeedback("#println-admin-txtt")) \\ "@onblur"
		val teaserCacheHandler = {
			val h = SHtml.ajaxTextarea("", setUnparsedTeaser(_).saveWithNoop) \\ "@onblur"
			h.toString.replaceAll("this.value", "\\$('#println-admin-txtt-cached').val()")
		}

		println("path: %s".format(Props.get("upload.path")))

		".println_post_name" #>					SHtml.ajaxText(post.name, saveName(post, _)) &
		".println_post_tags" #>					<xml:group>{tags(post)}</xml:group> &
		".println_post_teaser_link" #>			SHtml.ajaxText(post.teaserLink, post.teaserLink(_).saveWithJsFeedback(".println_post_teaser_link input")) &
		".println_post_publish_now" #>			SHtml.ajaxCheckbox(post.published, setPublished(post, _)) &
		".println_post_publish_date" #>			SHtml.ajaxText(post.publishDate.toFormattedString, savePublishDate(post, _)) &
		"#println-admin-txtc" #>				<textarea onblur={"%s; %s".format(contentHandler, contentCacheHandler)}>{post.content}</textarea> &
		"#println-admin-txtt" #>				<textarea onblur={"%s; %s".format(teaserHandler, teaserCacheHandler)}>{post.teaser}</textarea> &
		".println_post_slug" #>					SHtml.ajaxText(post.slug, saveSlug(post, _),
													if (post.published) "disabled" -> "disabled" else "style" -> "",
													if (post.slug == "post") "class" -> "invalid" else "style" -> "")
	}
	
	def delete = a(() => post.deleteWithJsFeedback("article[id=post_%s]".format(post.id), post.name), Text("permanently delete this Post"))

	def add = {
		def addPost(n: String) = {
			val p = Post.create.name(n).slug(HtmlHelpers.slugify(n))
			if (p.validate.length == 0 && p.save)
				RedirectTo("/%s".format(p.slug))
			else
				JsFx.failed("#post-name") &
				JsFx.invalidated("#post-name")
		}
		
		val addHandler = (SHtml.ajaxText("a", addPost(_)) \\ "@onblur").toString.replaceAll("this.value", "post")
		

		<div id="new-post" title="New Blog Post">
			{Script(JsRaw("println.post.add=function(){var post=$('#new-post-input').val();console.log(\"New Post: \"+post);%s;};".format(addHandler)))}
			<form onsubmit="javascript:println.post.add(); return false;">
			<fieldset>
				<label for="name">Name</label>
				<input type="text" name="new-post-name" id="new-post-input" class="text ui-widget-content ui-corner-all" />
			</fieldset>
			</form>
		</div>
	}

}
