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

import code.model._
import code.lib._


object SpeedTestAgent {
	def apply(): Boolean = {
		try {
			if (S.request.open_!.userAgent.open_!.matches(".*(GoogleBot|Google Page Speed).*"))
				return true
		} catch {
			case _ => return true
		}
		false
	}

	def hide(xhtml: NodeSeq) = if (apply()) NodeSeq.Empty else xhtml
	def show(xhtml: NodeSeq) = if (apply()) xhtml else NodeSeq.Empty

}

class Helpers extends Loggable {

	/* snippets */
	def disqus: NodeSeq = S.attr("name") match {
		case Full(dn: String) if dn != "" =>
			if (SpeedTestAgent()) return NodeSeq.Empty
			<div id="disqus_thread">
				<script src={"http://%s.disqus.com/embed.js".format(dn)} type="text/javascript"></script>
				<noscript>
					<a href={"http://%s.disqus.com/embed.js?url=ref".format(dn)}>View comments.</a>
				</noscript>
			</div>

		case _ => NodeSeq.Empty
	}


	def analytics: NodeSeq = S.attr("ua") match {
		case Full(ua: String) if (ua != "") =>
			if (SpeedTestAgent()) return NodeSeq.Empty
			Script(JsRaw("""
				var _gaq = _gaq || [];
				_gaq.push(['_setAccount', '%s']);
				_gaq.push(['_trackPageview']);
				(function() {
					var ga = document.createElement('script'); ga.type = 'text/javascript'; ga.async = true;
					ga.src = ('https:' == document.location.protocol ? 'https://ssl' : 'http://www') + '.google-analytics.com/ga.js';
					var s = document.getElementsByTagName('script')[0]; s.parentNode.insertBefore(ga, s);
				})();""".format(ua.replaceAll("^(UA-)?", "UA-"))))

		case _ => NodeSeq.Empty
	}


	def years: CssSel = {
		var years = DateTimeHelpers.getDate.toString("yyyy")
	
		S.attr("since") match {
			case Full(y: String) => {
				val year = y.toInt
				val currentYear = years.toInt
				if (year >= currentYear)
					years = currentYear.toString
				else
					years = "%s-%s".format(year, currentYear)
			}
			case _ =>
		}
		
		"*" #> years
	}
	
	
	def bitpit: NodeSeq = S.attr("id") match {
		case Full(id: String) if (id != "") =>
			if (SpeedTestAgent()) return NodeSeq.Empty
			<xml:group>
				<script type="text/javascript" src="http://api.bitp.it/bitp.it.js"></script>
	      		{Script(JsRaw("bitpit({clientId: '%s', forceUIThread: true});".format(id)))}
			</xml:group>
	
		case _ => NodeSeq.Empty
	}
	
	
	def twitter: NodeSeq = S.attr("user") match {
		case Full(tu: String) if (tu != "") =>
			if (SpeedTestAgent()) return NodeSeq.Empty
			<xml:group>
				<h3>Twitter</h3>
	        
				<script src="http://widgets.twimg.com/j/2/widget.js"></script>
				{Script(JsRaw("""
					new TWTR.Widget({
						version: 2,
						type: 'profile',
						rpp: 4,
						interval: 6000,
						width: 200,
						height: 360,
						theme: {
							shell: {
								background: '#ffffff',
								color: '#0d0b0d'
							},
							tweets: {
								background: '#ffffff',
								color: '#000000',
								links: '#3551de'
							}
						},
						features: {
							scrollbar: false,
							loop: false,
							live: true,
							hashtags: false,
							timestamp: true,
							avatars: false,
							behavior: 'all'
						}
					}).render().setUser('%s').start();
					""".format(tu)))}
			</xml:group>
	
		case _ => NodeSeq.Empty
	}

}
