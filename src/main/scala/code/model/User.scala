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

import net.liftweb.http.{S, SessionVar}
import net.liftweb.sitemap.Menu
import net.liftweb.util._
import net.liftweb.common._
import net.liftweb.mapper._
import net.liftweb.mongodb._
import net.liftweb.mongodb.record._
import net.liftweb.mongodb.record.field._
import net.liftweb.record._
import net.liftweb.record.field._

import scala.xml._
import java.util.{Date, Calendar}
import org.joda.time.{DateTime, DateTimeZone}
import com.foursquare.rogue.Rogue._


class User extends net.liftweb.mapper.MegaProtoUser[User] {
	def getSingleton = User

	object name extends net.liftweb.mapper.MappedString(this, 255)

	object locked extends net.liftweb.mapper.MappedBoolean(this) {
		override def dbNotNull_? = true
		override def defaultValue = false
	}

	object publisher extends net.liftweb.mapper.MappedBoolean(this) {
		override def dbNotNull_? = true
		override def defaultValue = false
	}

	object moderator extends net.liftweb.mapper.MappedBoolean(this) {
		override def dbNotNull_? = true
		override def defaultValue = false
	}

}

object User extends User with net.liftweb.mapper.MetaMegaProtoUser[User] {
	override def dbTableName = "users"
	override def signupFields = List(email, password)

	override val basePath: List[String] = "users" :: Nil
	override def skipEmailValidation = true

	//override def editUserMenuLoc: Box[Menu] = Empty
	override def validateUserMenuLoc: Box[Menu] = Empty
	override def lostPasswordMenuLoc: Box[Menu] = Empty
	override def resetPasswordMenuLoc: Box[Menu] = Empty
	//override def changePasswordMenuLoc: Box[Menu] = Empty

	object loginReferer extends SessionVar("/")

	override def homePage = {
		var ret = loginReferer
		loginReferer.remove()
		ret
	}

	override def login = {
		for (refererPath <- S.referer if loginReferer.is == "/") loginReferer.set(refererPath)
		super.login
	}

	def isAdmin_?(): Boolean = this.currentUser match {
		case Full(u: User) => (u.superUser)
		case _ => false
	}
	
	def isLoggedIn_?(): Boolean = this.currentUser match {
		case Full(u: User) => true
		case _ => false
	}

	override def screenWrap = Full(
		<lift:surround with="default" at="main">
			<lift:bind/>
		</lift:surround>
	)

	override def loginXhtml = {
		(<span>
			<h1>{S.??("log.in")}</h1>
			<form method="post" action={S.uri}>
				<table>
					<tr><th>{S.??("email.address")}</th><td><user:email /></td></tr>
					<tr><th>{S.??("password")}</th><td><user:password /></td></tr>
					<tr><td></td><td><user:submit /></td></tr>
				</table>
			</form>
		</span>)
	}

	override def signupXhtml(user: TheUserType) = {
		if (User.findAll().length >= 1)
			<span/>
		else
			(<form method="post" action={S.uri}>
				<h1>{ S.??("sign.up") }</h1>
				<table>
					{localForm(user, false, signupFields)}
					<tr><td> </td><td><user:submit/></td></tr>
				</table>
			</form>)
	}

	override def lostPasswordXhtml = {
		(<form method="post" action={S.uri}>
			<h1>{S.??("enter.email")}</h1>
			<table>
				<tr><th>{S.??("email.address")}</th><td><user:email /></td></tr>
				<tr><td> </td><td><user:submit /></td></tr>
			</table>
		</form>)
	}

	override def passwordResetXhtml = {
    	(<form method="post" action={S.uri}>
			<h1>{S.??("reset.your.password")}</h1>
        	<table>
        		<tr><th>{S.??("enter.your.new.password")}</th><td><user:pwd/></td></tr>
				<tr><th>{S.??("repeat.your.new.password")}</th><td><user:pwd/></td></tr>
				<tr><td> </td><td><user:submit/></td></tr>
			</table>
		</form>)
 	}

	override def changePasswordXhtml = {
		(<form method="post" action={S.uri}>
			<h1>{S.??("change.password")}</h1>
			<table>
				<tr><th>{S.??("old.password")}</th><td><user:old_pwd /></td></tr>
				<tr><th>{S.??("new.password")}</th><td><user:new_pwd /></td></tr>
				<tr><th>{S.??("repeat.password")}</th><td><user:new_pwd /></td></tr>
				<tr><td> </td><td><user:submit /></td></tr>
			</table>
		</form>)
	}

	private def setSuperUser(u: User) = {
		if (User.count() == 0) u.superUser(true)
		()
	}

	override def beforeCreate = setSuperUser _ :: super.beforeCreate
}
