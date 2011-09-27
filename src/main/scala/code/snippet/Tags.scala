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


import net.liftweb.sitemap._
import net.liftweb.sitemap.Loc._

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


class Tags extends Loggable {
  
	lazy val tag = Tag.find(By(Tag.slug, S.uri.replaceFirst("^/tag/", ""))).open_!

	/* snippets */
	def name = Text(tag.name)
	def title = <title>{HtmlHelpers.subtitle(tag.name)}</title>

	def articles: CssSel =
		"li *" #> tag.listPosts.map(p =>
			"article [id]" #>							"post_%s".format(p.id) &
			".println_entry_link [href]" #>				p.link &
			".println_entry_link *" #>					p.name &
			".println_entry_teaser_link [href]" #>		p.link &
			".println_entry_teaser_link *" #>			p.teaserLink &
			".println_post_footer *" #>					DateTimeHelpers.postFooter(p) &
			".println_post_footer [id]" #>				"println_entry_footer_%s".format(p.id) &
			".println_entry_teaser *" #>				p.teaserText)

	def cloud = {
		val tags = DB.runQuery("""
			SELECT COUNT(pt.tag) AS count, t.name AS tag, t.slug AS slug
			FROM post_tags AS pt LEFT JOIN tags AS t
			ON (t.id = pt.tag)
			GROUP BY t.name, t.slug ORDER BY count, t.name
		""")
		val min = try {
			DB.runQuery("SELECT COUNT(tag) AS count, tag FROM post_tags GROUP BY tag ORDER BY count ASC LIMIT 1")._2(0)(0).toInt
		} catch {
			case _ => 0
		}
		val max = try {
			DB.runQuery("SELECT COUNT(tag) AS count, tag FROM post_tags GROUP BY tag ORDER BY count DESC LIMIT 1")._2(0)(0).toInt
		} catch {
			case _ => 0
		}
		val diff = max - min
		val distri = diff / 3

		tags._2.map(t => {
			var cssclass = "smallTag"
			val count = t(0).toInt
			if      (count == min)				cssclass = "smallestTag"
			else if (count == max)				cssclass = "largestTag"
			else if (count > min + distri *2)	cssclass = "largeTag"
			else if (count > min + distri)		cssclass = "mediumTag"

			<a class={cssclass} href={if (User.loggedIn_?) "/admin" else "" + "/tag/%s".format(t(2))}>{t(1)}</a>
		})
	}

}
