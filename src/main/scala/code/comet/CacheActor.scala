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

package code.comet

import net.liftweb.http._
import net.liftweb.actor._
import net.liftweb.util._
import net.liftweb.util.Helpers._

import org.joda.time._
import scala.collection.immutable.Map

import code.model._


case class GetResponse(uri: String)
case class StoreResponse(uri: String, resp: LiftResponse)
case class CleanupCache

case class CachedReply(resp: LiftResponse, updated: DateTime)


object CacheActor extends LiftActor {

	var responses: Map[String, CachedReply] = Map()

	ActorPing.schedule(this, CleanupCache, 1 minute)

	protected def messageHandler = {
		case StoreResponse(uri: String, resp: LiftResponse) =>
			responses = responses ++ Map(uri -> CachedReply(resp, new DateTime))

		case GetResponse(uri: String) =>
			responses.get(uri) match {
				case Some(a: CachedReply) => reply(a)
				case _ => reply(null)
			}

		case CleanupCache =>
			val cleanDate = (new DateTime).minusMinutes(1)
			responses = responses.filter(_._2.updated.isAfter(cleanDate))

			ActorPing.schedule(this, CleanupCache, 1 minute)

	}

}