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


class Sitemap extends Loggable {

	/* snippets */
	def base: CssSel =
		"loc *" #>				"http://%s".format(S.hostName)  &
		"lastmod *" #>			Post.lastmod.toString("yyyy-MM-dd'T'HH:mm:ss.SSSZZ") // manual to avoid errors!

	def list: CssSel =
		"url *" #> Post.all.map(p =>
			"loc *" #>			"http://%s/%s".format(S.hostName, p.slug) &
			"lastmod *" #>		p.publishDate.toISOString)

	def atom: CssSel =
		"id *" #>					"tag:%s,2011:/".format(S.hostName) &
		"rel=self [href]" #>		"http://%s/atom.xml".format(S.hostName) &
		"rel=self [alternate]" #>	"http://%s".format(S.hostName) &
		"generator *" #>			"println" &
		"generator [uri]" #>		"http://println.io" &
		"updated *" #>				Post.lastmod.toString("yyyy-MM-dd'T'HH:mm:ss.SSSZZ") & // manual to avoid errors!
		"entry *" #> Post.all.map(p =>
			"title *" #>			p.name &
			"link [href]" #>		"http://%s/%s".format(S.hostName, p.slug) &
			"published *" #>		p.publishDate.toISOString &
			"updated *" #>			p.updatedAt.toISOString &
			"id *" #>				"tag:%s,%s:/".format(S.hostName, p.publishDate.toString("yyyy-MM-dd"), p.slug) &
			"content *" #> {
				try {
					val tcns = HtmlHelpers.filter(p.teaserCache.is)
					XML.loadString("<span>" + tcns + "</span>")
				} catch {
					case _ => NodeSeq.Empty
				}
			})

}
