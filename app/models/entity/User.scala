package models.entity

import java.util.{Base64, Date}
import java.util.concurrent.TimeUnit

import authentikat.jwt.{JsonWebToken, JwtClaimsSet, JwtHeader}
import models.form.NewPasswordForm
import org.mindrot.jbcrypt.BCrypt

import scala.concurrent.duration.{Duration, FiniteDuration}

case class User(id: Option[Int], email: String, password: String, active: Boolean = true)

object UserPasswordManager {

  val duration = FiniteDuration(3, TimeUnit.HOURS)

  def emailFromJwt(jwt: String, secret: String): Option[String] = {
    val valid = new String(Base64.getDecoder.decode(jwt.getBytes))
    println(valid)
    val claims = parseValidJwt(valid, secret)
    getEmailIfValid(claimSet = claims)
  }

  private def getEmailIfValid(claimSet: Option[Map[String, String]]): Option[String] = claimSet match {
    case Some(claims) => if (isTimeOver( claims("created")) ) None else Some(claims("email"))
    case None => None
  }

  private def isTimeOver(timestamp: String) = {
    val created = Duration(timestamp.toLong, TimeUnit.MILLISECONDS)
    val now = Duration(new Date().getTime, TimeUnit.MILLISECONDS)
    val isLessThen3Hours = created.plus(duration) <= now
    isLessThen3Hours
  }

  private def parseValidJwt(jwt: String, secret: String): Option[Map[String, String]] = {
    if (JsonWebToken.validate(jwt, secret)) jwt match {
      case JsonWebToken(header, claimsSet, signature) => claimsSet.asSimpleMap.toOption
      case x => None
    } else None
  }

  def generateEmailAndCreatedJwt(email: String, secret: String) = {
    val header = JwtHeader("HS256")
    val claimsSet = JwtClaimsSet(Map(
      "email" -> email,
      "created" -> Long.box(new Date().getTime).toString)
    )
    val r = Base64.getEncoder.encodeToString(JsonWebToken(header, claimsSet, secret).getBytes)
    println("Return "+r)
    r
  }


  def validNewPassword(form: NewPasswordForm) = form.newPassword.length > 5 && form.newPassword == form.repeatedNewPassword

  def passwordMatches(form: NewPasswordForm, user: User) = BCrypt.checkpw(form.password, user.password)

  def checkItAll(form: NewPasswordForm, user: User) = passwordMatches(form, user) && validNewPassword(form)

  def encryptPassword(password: String) = BCrypt.hashpw(password, BCrypt.gensalt())
}