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

package bootstrap.liftweb

import com.mongodb.{Mongo, MongoOptions, ServerAddress}

import net.liftweb._
import net.liftweb.common._
import net.liftweb.http._
import net.liftweb.util._
import net.liftweb.util.Helpers._
import net.liftweb.mapper._
import net.liftweb.widgets.flot._
import net.liftweb.sitemap._
import net.liftweb.sitemap.Loc._
import net.liftweb.mongodb._

import scala.xml._
import scala.collection.immutable.TreeMap

import code.lib._
import code.comet._
import code.model._
import code.snippet._


class Boot {
	def boot {

		if (!DB.jndiJdbcConnAvailable_?) {
			val vendor = new StandardDBVendor(
				Props.get("db.driver") openOr "org.postgresql.Driver",
				Props.get("db.url") openOr "jdbc:postgresql:printlndemo",
				Props.get("db.user"), Props.get("db.password"))

			LiftRules.unloadHooks.append(vendor.closeAllConnections_! _)

			DB.defineConnectionManager(DefaultConnectionIdentifier, vendor)
		}

		Schemifier.schemify(true, Schemifier.infoF _, User, Post, Tag, PostTags)

		System.setProperty("mail.smtp.host", Props.get("smtp.host") openOr "localhost")
		System.setProperty("mail.smtp.from", Props.get("smtp.from") openOr "noreply@i.didnt.configure.jack.shit")

		if (PrintlnMongo.enabled_?) {
			val srvr = new ServerAddress(Props.get("mo.host") openOr "127.0.0.1", Props.get("mo.port").openOr("27017").toInt)
			val mo = new MongoOptions
			mo.socketTimeout = 1000
			MongoDB.defineDb(DefaultMongoIdentifier, new Mongo(srvr, mo), Props.get("mo.db") openOr "printlndemo")
		}

		// where to search snippet
		LiftRules.addToPackages("code")

		val redirectUnlessUser = If(() => User.loggedIn_?, () => RedirectResponse("/"))
		val redirectUnlessAdmin = If(() => User.isAdmin_?, () => RedirectResponse("/"))
		val redirectUnlessMongo = If(() => PrintlnMongo.enabled_?, () => RedirectResponse("/"))

		// Build SiteMap
		def sitemap = SiteMap(
			Menu.i("New Post") / "admin" / "post" >> redirectUnlessAdmin,
			//Menu.i("Posts") / "index",
			Menu.i("Admin Posts") / "admin" / "index" >> redirectUnlessAdmin >> Hidden,
			Menu.i("Statistics") / "admin" / "stats" >> redirectUnlessUser >> redirectUnlessMongo,
			Menu.i("Users") / "admin" / "users" >> redirectUnlessAdmin >> User.AddUserMenusAfter
		)

		def sitemapMutators = User.sitemapMutator

		// Charting
		Flot.init

		// set the sitemap.	Note if you don't want access control for
		// each page, just comment this line out.
		LiftRules.setSiteMapFunc(() => sitemapMutators(sitemap))

		// Use jQuery 1.4
		LiftRules.jsArtifacts = net.liftweb.http.js.jquery.JQuery14Artifacts

		//Show the spinny image when an Ajax call starts
		LiftRules.ajaxStart =
			Full(() => LiftRules.jsArtifacts.show("ajax-loader").cmd)

		// Make the spinny image go away when it ends
		LiftRules.ajaxEnd =
			Full(() => LiftRules.jsArtifacts.hide("ajax-loader").cmd)

		// Force the request to be UTF-8
		LiftRules.early.append(_.setCharacterEncoding("UTF-8"))

		// What is the function to test if a user is logged in?
		LiftRules.loggedInTest = Full(() => User.loggedIn_?)

		// Make file uploads to be written onto disk instead of ram
		LiftRules.handleMimeFile = OnDiskFileParamHolder.apply

		// Use HTML5 for rendering
		LiftRules.htmlProperties.default.set((r: Req) =>
			new Html5Properties(r.userAgent))

		// Make a transaction span the whole HTTP request
		//S.addAround(DB.buildLoanWrapper)


		def renderTemplate(what: String) =
			S.render(<lift:embed what={what} />, S.request.get.request).first

		def normalizeURI(a: String) = a.replaceAll("(\\.[xht]+ml)?(\\?.*)?$", "")

		def cachedResponse_?(req: Req): Box[NotFound] = {
			if (User.loggedIn_?) return Empty
			CacheActor !! GetResponse(req, normalizeURI(req.uri)) match {
				case Full(rep: CachedReply) =>
					Full(NotFoundAsResponse(rep.resp))
				case _ => Empty
			}
		}

		def cacheResponse(req: Req, res: LiftResponse) = {
			if (!User.loggedIn_?)
				CacheActor ! StoreResponse(req, normalizeURI(req.uri), res)
			res
		}

		LiftRules.statelessTest.append {
			case "users" :: "login" :: Nil => false
			case "admin" :: _ => false
		}

		LiftRules.passNotFoundToChain = false
		LiftRules.uriNotFound.prepend {
			case _ => S.request match {
				case Full(req: Req) => {
					val cached = cachedResponse_?(req)
					req match {
						case _ if (cached != Empty) => cached.open_!
						case _ if (req.uri.matches("/(index(\\.html?)?)?")) =>
							NotFoundAsResponse(cacheResponse(req, XhtmlTemplateResponse(ParsePath("index" :: Nil, "html", false, false), 200)))

						case _ if (req.uri.matches("/atom(\\.xml)?")) =>
							NotFoundAsResponse(cacheResponse(req, AtomResponse(renderTemplate("atom"))))

						case _ if (req.uri.matches("/sitemap(\\.xml)?")) =>
							NotFoundAsResponse(cacheResponse(req, XmlResponse(renderTemplate("sitemap"), 200, "application/xml; charset=utf-8")))

						case _ if (req.uri.matches("^(/admin)?/tag/.+$") && Tag.findAll(By(Tag.slug, req.uri.replaceFirst("^(/admin)?/tag/", ""))).length != 0) =>
							NotFoundAsResponse(cacheResponse(req, XhtmlTemplateResponse(ParsePath("tag" :: Nil, "html", false, false), 200)))

						case _ if (Post.one(req.uri) != Empty) =>
							NotFoundAsResponse(cacheResponse(req, XhtmlTemplateResponse(ParsePath("post" :: Nil, "html", false, false), 200)))

						case _ => NotFoundAsResponse(XhtmlNotFoundResponse())
					}
				}
				case _ => NotFoundAsResponse(XhtmlNotFoundResponse())
			}
		}

	}
}


object XhtmlTemplateResponse extends HeaderDefaults {
	def apply(path: ParsePath, statusCode: Int): LiftResponse = {
		(for {
			session <- S.session
			template =  Templates(path.partPath)
			resp <- session.processTemplate(template, S.request.open_!, path, statusCode)
		} yield resp) match {
			case Full(resp) => resp
			case _ => XhtmlNotFoundResponse()
		}
	}
}


object XhtmlNotFoundResponse extends HeaderDefaults {
	def apply() = XhtmlTemplateResponse(ParsePath("404" :: Nil, "html", false, false), 404)
}

