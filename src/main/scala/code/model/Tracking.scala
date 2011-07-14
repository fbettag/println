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

package code.model

import net.liftweb.http.{S, Req}
import net.liftweb.util._
import net.liftweb.common._
import net.liftweb.mongodb._
import net.liftweb.mongodb.record._
import net.liftweb.mongodb.record.field._
import net.liftweb.record._
import net.liftweb.record.field._

import java.util.{Date, Calendar}
import org.joda.time.{DateTime, DateTimeZone}
import com.foursquare.rogue.Rogue._

import code.lib._


class StatAttrs(len: Int, hour: Int, month: Int) {
	val length = len
	val hours = hour
	val months = month
}


class WebTrack private() extends MongoRecord[WebTrack] with MongoId[WebTrack] {
	def meta = WebTrack

	object url extends ObjectIdRefField(this, WebView)
	object browser extends ObjectIdRefField(this, WebBrowser)
	object referer extends ObjectIdRefField(this, WebReferer)
	object session extends ObjectIdRefField(this, WebSession)
	object timestamp extends DateTimeField(this)

}

object WebTrack extends WebTrack with MongoMetaRecord[WebTrack] {

	def track = {
		/* browser tracking */
		val browser = S.request match {
			case Full(r: Req) => {
				val browser = WebBrowser.countUp(r.userAgent.openOr("none"))

				/* view tracking */
				val url = WebView.countUp(r.uri)

				/* referer tracking ONLY if NOT pixforce */
				val referer = S.referer.openOr("direct")

				val session = WebSession.countUp(S.containerSession.map(_.sessionId).openOr(""))
				
				if (referer.matches("^http://*pixforce.de.*$")) {
					WebTrack.createRecord.timestamp(Calendar.getInstance).browser(browser.id).url(url.id).session(session.id).save
				} else {
					var ref = WebReferer.from(referer)
					WebTrack.createRecord.timestamp(Calendar.getInstance).browser(browser.id).url(url.id).session(session.id).referer(ref.id).save
				}
			}
			case _ =>
		}
	}
	
	val today = DateTimeHelpers.getDate

	def currentRPM = (WebTrack where (_.timestamp after DateTimeHelpers.getDate.minusMinutes(1)) count())

	def viewsToday = {
		val yesterday = today.minusDays(1)
		WebTrack where (_.timestamp after yesterday) count()
	}

	def viewsYesterday = {
		val yesterday = today.minusDays(1)
		val before = today.minusDays(2)
		WebTrack where (_.timestamp after yesterday) and (_.timestamp before before) count()
	}

	def viewsWeek = {
		val week = today.minusWeeks(1)
		WebTrack where (_.timestamp after week) count()
	}

	def viewsTotal = {
		WebTrack count()
	}


	def currentVisitors = (WebSession where (_.lastVisit after DateTimeHelpers.getDate.minusMinutes(10)) count())

	def visitorsToday = {
		val yesterday = today.minusDays(1)
		WebSession where (_.lastVisit after yesterday) count()
	}

	def visitorsYesterday = {
		val yesterday = today.minusDays(1)
		val before = today.minusDays(2)
		WebSession where (_.lastVisit after yesterday) and (_.lastVisit before before) count()
	}

	def visitorsWeek = {
		val week = today.minusWeeks(1)
		WebSession where (_.lastVisit after week) count()
	}

	def visitorsTotal = WebSession count()
	
}


class WebSession private() extends MongoRecord[WebSession] with MongoId[WebSession] {
	def meta = WebSession

	object name extends StringField(this, 100)
	object lastVisit extends DateTimeField(this)

}

object WebSession extends WebSession with MongoMetaRecord[WebSession] {

	def countUp(name: String): WebSession = WebSession where (_.name eqs name) fetch(1) match {
		case l: List[WebSession] if l.length > 0 => {
			val sess = l.head
			WebSession where (_._id eqs sess.id) modify (_.lastVisit setTo DateTimeHelpers.getDate) upsertOne()
			sess
		}
		case _ => {
			val sess = WebSession.createRecord.name(name).lastVisit(Calendar.getInstance)
			sess.save
			sess
		}
	}

}


class WebView private() extends MongoRecord[WebView] with MongoId[WebView] {
	def meta = WebView

	object name extends StringField(this, 512)
	object views extends IntField(this)
	object lastView extends DateTimeField(this)

}

object WebView extends WebView with MongoMetaRecord[WebView] {

	def countUp(name: String): WebView = WebView where (_.name eqs name) fetch(1) match {
		case l: List[WebView] if l.length > 0 => {
			val view = l.head
			WebView where (_._id eqs view.id) modify (_.views inc 1) upsertOne()
			view
		}
		case _ => {
			val view = WebView.createRecord.name(name).views(1).lastView(Calendar.getInstance)
			view.save
			view
		}
	}

	def top(num: Int, hours: Int) = WebView where (_.lastView after DateTimeHelpers.getDate.minusHours(hours)) orderDesc(_.views) fetch(num)
	def top(attrs: StatAttrs): List[WebView] = top(attrs.length, attrs.hours)

}


class WebBrowser private() extends MongoRecord[WebBrowser] with MongoId[WebBrowser] {
	def meta = WebBrowser

	object name extends StringField(this, 255)
	object views extends IntField(this)
	object lastVisit extends DateTimeField(this)

}

object WebBrowser extends WebBrowser with MongoMetaRecord[WebBrowser] {

	def countUp(browserString: String): WebBrowser = WebBrowser where (_.name eqs browserString) fetch(1) match {
		case l: List[WebBrowser] if l.length > 0 => {
			val browser = l.head
			WebBrowser where (_.name eqs browserString) modify (_.views inc 1) and (_.lastVisit setTo DateTimeHelpers.getDate) upsertOne()
			browser
		}
		case _ => {
			val browser = WebBrowser.createRecord.name(browserString).lastVisit(Calendar.getInstance).views(1)
			browser.save
			browser
		}
	}
	
	def top(num: Int, months: Int) = WebBrowser where (_.lastVisit after DateTimeHelpers.getDate.minusMonths(months)) orderDesc(_.views) fetch(num)
	def top(attrs: StatAttrs): List[WebBrowser] = top(attrs.length, attrs.months)

}


class WebReferer private() extends MongoRecord[WebReferer] with MongoId[WebReferer] {
	def meta = WebReferer

	object name extends StringField(this, 512)
	object refers extends IntField(this)
	object lastRefered extends DateTimeField(this)

}

object WebReferer extends WebReferer with MongoMetaRecord[WebReferer] {

	def from(name: String) = WebReferer where (_.name eqs name) fetch(1) match {
		case l: List[WebReferer] if l.length > 0 => {
			val ref = l.head
			WebReferer where (_.name eqs name) modify (_.refers inc 1) and (_.lastRefered setTo DateTimeHelpers.getDate) upsertOne()
			ref
		}
		case _ => {
			val ref = WebReferer.createRecord.name(name).refers(1).lastRefered(Calendar.getInstance)
			ref.save
			ref
		}
	}

	def top(num: Int, hours: Int): List[WebReferer] = WebReferer where (_.lastRefered after DateTimeHelpers.getDate.minusHours(hours)) orderDesc(_.refers) fetch(num)
	def top(attrs: StatAttrs): List[WebReferer] = top(attrs.length, attrs.hours)

}


