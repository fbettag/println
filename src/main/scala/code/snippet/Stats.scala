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
import net.liftweb.widgets.flot._

import scala.xml._
import java.util.{Date, Calendar}
import org.joda.time.{DateTime, DateTimeZone}
import com.foursquare.rogue.Rogue._

import code.model._
import code.lib._


class Stats extends Loggable {

	private var listStore: Box[CssSel] = Empty


	/* helpers */
	private def getAttrsWithDefaults(len: Int, time: Int) = {
		val length = S.attr("length") match {
			case Full(l: String) => l.toInt
			case _ => len
		}
		val hours = S.attr("hours") match {
			case Full(l: String) => l.toInt
			case _ => time
		}
		val months = S.attr("months") match {
			case Full(l: String) => l.toInt
			case _ => time
		}
		
		new StatAttrs(length, hours, months)
	}
	

	/* snippets */
	def title = <title>{HtmlHelpers.title("Statistics")}</title>
	
	def track = {
		try { WebTrack.track } catch { case _ => }
		NodeSeq.Empty	
	}
	
	def graph(xhtml: NodeSeq) = {
		var maxHeight: Long = 0
		def setMax(f: Long) = if (f > maxHeight) maxHeight = f

		val visitors = new FlotSerie() {
			override def label = Full(S.??("Besucher"))
			override val data: List[(Double,Double)] = 
				List.range(0, 60*25, 15).map(minute => {
					val stopTime = DateTimeHelpers.getDate.minusMinutes(minute)
					val startTime = stopTime.minusMinutes(minute+15)
					val sessions = WebSession where (_.lastVisit before stopTime) and (_.lastVisit after startTime) count()
					setMax(sessions)
					((stopTime.getMillis + DateTimeHelpers.getOffsetMillis).toDouble, sessions.toDouble)
				}).toList
		}

		val views = new FlotSerie() {
			override def label = Full(S.??("Ansichten"))
			override val data: List[(Double,Double)] = 
				List.range(0, 60*25, 15).map(minute => {
					val stopTime = DateTimeHelpers.getDate.minusMinutes(minute)
					val startTime = stopTime.minusMinutes(minute+15)
					val sessions = WebTrack where (_.timestamp before stopTime) and (_.timestamp after startTime) count()
					setMax(sessions)
					((stopTime.getMillis + DateTimeHelpers.getOffsetMillis).toDouble, sessions.toDouble)
				}).toList
		}
		
		val options: FlotOptions = new FlotOptions () {
			override val xaxis = Full(new FlotAxisOptions() {
				override val mode = Full("time")
			})
			
			override val yaxis = Full(new FlotAxisOptions() {
				override val max = Full(maxHeight*1.3)
			})
			
			override val legend = Full(new FlotLegendOptions() {
				override val backgroundColor = Full("transparent")
				override val position = Full("nw")
			})
		}

		Flot.render("visitors_graph_area", List(visitors, views), options, Flot.script(xhtml))
	}


	// We cache this since we might need .list more than once on a page
	def list = listStore match {
		case Full(a: CssSel) => a
		case _ => {
			listStore = Full(
				"#visitors_today *" #> WebTrack.visitorsToday &
				"#visitors_yesterday *" #> WebTrack.visitorsYesterday &
				"#visitors_week *" #> WebTrack.visitorsWeek &
				"#visitors_total *" #> WebTrack.visitorsTotal &
				"#views_today *" #> WebTrack.viewsToday &
				"#views_yesterday *" #> WebTrack.viewsYesterday &
				"#views_week *" #> WebTrack.viewsWeek &
				"#views_total *" #> WebTrack.viewsTotal &
				"#current_rpm *" #> WebTrack.currentRPM &
				"#current_visitors *" #> WebTrack.currentVisitors
			)
			listStore.open_!
		}
	}


	def webviews = {
		val webviews = WebView.top(getAttrsWithDefaults(10, 24))
		".webview_row *" #> webviews.map(webview => {
			".webview_url *" #> webview.name.is &
			".webview_views *" #> webview.views.is &
			".webview_lastView *" #> new DateTime(webview.lastView.is).toString("YYYY-MM-dd HH:mm:ss")
		})
	}

	def referers = {
		val referers = WebReferer.top(getAttrsWithDefaults(10, 24*7))
		".referer_row *" #> referers.map(referer => {
			".referer_name *" #> referer.name.is &
			".referer_refers *" #> referer.refers.is &
			".referer_lastRefered *" #> new DateTime(referer.lastRefered.is).toString("YYYY-MM-dd HH:mm:ss")
		})
	}

	def browsers = {
		val browsers = WebBrowser.top(getAttrsWithDefaults(10, 3))
		".browser_row *" #> browsers.map(browser => {
			".browser_name *" #> browser.name.is &
			".browser_views *" #> browser.views.is &
			".browser_lastVisit *" #> new DateTime(browser.lastVisit.is).toString("YYYY-MM-dd HH:mm:ss")
		})
	}
		
}
