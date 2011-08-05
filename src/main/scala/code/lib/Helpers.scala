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

package code.lib

import net.liftweb.common._
import net.liftweb.util._
import net.liftweb.util.Helpers._
import net.liftweb.http._
import net.liftweb.http.SHtml._
import net.liftweb.http.js._
import net.liftweb.http.js.JE._
import net.liftweb.http.js.JsCmds._
import net.liftweb.http.js.JE.JsRaw
import net.liftweb.http.js.JsCmd
import net.liftweb.mapper._

import java.util.{Date, Calendar, TimeZone}
import org.joda.time.{DateTime, DateTimeZone}
import scala.xml._

import ag.bett.scala.lib.Exec._

import code.model._

object DateTimeHelpers {

	var timezone = "CET"

	def getTZ(tz: String): DateTimeZone = DateTimeZone.forTimeZone(TimeZone.getTimeZone(tz))
	def getUserTZ: DateTimeZone = User.currentUser match {
		case Full(u: User) => getTZ(u.timezone)
		case _ => getTZ(timezone)
	}

	def getDate: DateTime =

		new DateTime(User.currentUser match {
			case Full(u: User) => getTZ(u.timezone)
			case _ => getTZ(timezone)
		})

	def getDate(date: Calendar): DateTime = new DateTime(date, getUserTZ)
	def getDate(date: Date): DateTime = new DateTime(date, getUserTZ)

	def getOffsetMillis = getUserTZ.getOffset(0)
	def getOffsetSeconds = getOffsetMillis/1000

	def postFooter(p: Post): NodeSeq = {
		val pubDate = p.publishDate.toRichString
		val pubDateISO = p.publishDate.toISOString

		<p class="meta">
			<xml:group>
				Tags: {XML.loadString("<xml:group>" + p.tags.map(t => "<a href=\"/tag/%s\">%s</a>".format(t.slug, t.name)).mkString(", ") + "</xml:group>")}
			</xml:group>
			<br/>

			{if (p.published.is) {
				if (p.publishDate.is.before(new Date))
					<xml:group>Published on <time datetime={pubDateISO} pubdate="pubdate">{pubDate}</time></xml:group>
				else
					<xml:group>>Will be published on <time datetime={pubDateISO} pubdate="pubdate">{pubDate}</time></xml:group>

			}
			else <xml:group>This Post is not published yet.</xml:group>}
		</p>
	}

	def updateTimestamps(p: Post): JsCmd =
		SetHtml("println_entry_footer_%s".format(p.id), DateTimeHelpers.postFooter(p)) &
		JsRaw("$('.println_post_publish_date').each(function(i, e) { $(e).val('%s'); })".format(p.publishDate.toFormattedString)).cmd &
		JsRaw("$('.println_post_publish_now').each(function(i, e) { $(e).attr('checked', %s); })".format(if (p.published) "'checked'" else "false")).cmd

}

object HtmlHelpers {

	def filter(a: String) = a.replaceAll("<(hr|br)>", "<$1/>")

	def slugify(a: String) =
		a.replaceAll(" ", "-").
		replaceAll("%E2%82%AC", "euro").
        replaceAll("%C3%84", "ae").
        replaceAll("%C3%A4", "ae").
        replaceAll("%C3%96", "oe").
        replaceAll("%C3%B6", "oe").
        replaceAll("%C3%9C", "ue").
        replaceAll("%C3%BC", "ue").
        replaceAll("%C3%9F", "ss").
        replaceAll("%26", "und").
        replaceAll("@", "-at-").
        replaceAll("[^a-zA-Z0-9]+", "-").
        replaceAll("-+", "-").
        replaceAll("(^\\-*|\\-*$)", "")

	def title(title: String) =
		Props.get("title") match {
			case Full(t: String) => "%s: %s".format(t, title)
			case _ => title
		}

}

object JsFx {

	def success(selector: String): JsCmd =
		JsRaw("$('%s').effect('highlight', {times: 2}, 400)".format(selector)).cmd

	def failed(selector: String): JsCmd =
		JsRaw("$('%s').effect('pulsate', {times: 4}, 200)".format(selector)).cmd

	def validated(selector: String): JsCmd =
		JsRaw("$('%s').removeClass('invalid')".format(selector)).cmd

	def invalidated(selector: String): JsCmd =
		JsRaw("$('%s').addClass('invalid')".format(selector)).cmd

	def remove(selector: String): JsCmd =
		JsRaw("$('%s').fadeOut(200)".format(selector)).cmd

}

trait JsEffects[A <: Mapper[A]] {
	this: A =>

	def jsFeedback(selector: String): JsCmd =
		if (this.saved_?)

			JsFx.success(selector) & JsFx.validated(selector)
		else JsFx.failed(selector) & JsFx.invalidated(selector)

	def saveWithJsFeedback(selector: String): JsCmd = {
		this.save
		this.jsFeedback(selector)
	}

	def saveWithNoop: JsCmd = { this.save; Noop }

	def delete_!!(selector: String) =
		if (this.delete_!) RedirectTo("/") //JsFx.remove(selector)
		else JsFx.failed(selector)

	private def deleteJs(selector: String): JsCmd = {
		val handler =  (SHtml.ajaxButton("delete", () => this.delete_!!(selector)) \\ "@onclick").toString.replaceAll("&quot;", "'").replaceAll("return false;+$", "")
		println("-----------------------\n%s\n%s\n-------------------".format(handler, JsRaw(handler).cmd))
		JsRaw(handler).cmd
	}

	def deleteWithJsFeedback(selector: String, name: String): JsCmd =
		Confirm("Permanently delete '%s'?".format(name), deleteJs(selector))

	def deleteWithJsFeedback(selector: String): JsCmd =
		Confirm("Are you sure?", deleteJs(selector))

}

trait FBDateTimeMapper {
  self: BaseMapper =>

	protected class FBMappedSlug(obj: self.type) extends MappedDateTime(obj.asInstanceOf[MapperType]) with LifecycleCallbacks {
		lazy val toDateTime = DateTimeHelpers.getDate(this)
		def toString(format: String) = toDateTime.toString(format)
		def toFormattedString = this.toString("yyyy-MM-dd HH:mm")
		def toRichString = this.toString("d '%s' yyyy HH:mm".format(toDateTime.monthOfYear().getAsText()))
		def toISOString = this.toString("yyyy-MM-dd'T'HH:mm:ss.SSSZZ")
	}

	protected class FBMappedDateTime(obj: self.type) extends MappedDateTime(obj.asInstanceOf[MapperType]) with LifecycleCallbacks {
		lazy val toDateTime = DateTimeHelpers.getDate(this)
		def toString(format: String) = toDateTime.toString(format)
		def toFormattedString = this.toString("yyyy-MM-dd HH:mm")
		def toRichString = this.toString("d '%s' yyyy HH:mm".format(toDateTime.monthOfYear().getAsText()))
		def toISOString = this.toString("yyyy-MM-dd'T'HH:mm:ss.SSSZZ")
	}

	protected class FBMappedUpdatedAt(obj: self.type) extends FBMappedDateTime(obj.asInstanceOf[self.type]) with LifecycleCallbacks {
		override def beforeSave() {super.beforeSave; this.set(DateTimeHelpers.getDate.toDate)}
		override def defaultValue = DateTimeHelpers.getDate.toDate
		override def dbIndexed_? = true
	}

	protected class FBMappedCreatedAt(obj: self.type) extends FBMappedDateTime(obj.asInstanceOf[self.type]) with LifecycleCallbacks {
		override def defaultValue = DateTimeHelpers.getDate.toDate
		override def dbIndexed_? = false
	}

}
