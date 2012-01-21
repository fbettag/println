/** {{{
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
 */// }}}

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


class Blog extends Loggable {

	lazy val publicPostCount = Post.count(By(Post.published, true), By_<(Post.publishDate, new Date))

	/* snippets */
	def step1register(xhtml: NodeSeq) = if (User.count() == 0) xhtml else NodeSeq.Empty
	def step1login(xhtml: NodeSeq) = if (!User.isLoggedIn_? && User.count() > 0 && publicPostCount == 0) xhtml else NodeSeq.Empty
	def step2(xhtml: NodeSeq) = if (User.isLoggedIn_? && Post.count() == 0) xhtml else NodeSeq.Empty

	def title: NodeSeq = <title>{HtmlHelpers.title}</title>

	def articles: CssSel =
		if (Post.count() == 0)
			"section" #> ""
		else
			"li *" #> Post.all.map(p =>
				"article [id]" #>							"post_%s".format(p.id) &
				".println_entry_link [href]" #>				p.link &
				".println_entry_link *" #>					p.name &
				".println_entry_teaser_link [href]" #>		p.link &
				".println_entry_teaser_link *" #>			p.teaserLink &
				".println_post_footer *" #>					DateTimeHelpers.postFooter(p) &
				".println_post_footer [id]" #>				"println_entry_footer_%s".format(p.id) &
				".println_entry_teaser *" #>				p.teaserText)

}

