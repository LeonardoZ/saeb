package models.mail

import javax.inject.Inject

import play.api.libs.mailer.{Email, MailerClient}

import scala.concurrent.{ExecutionContext, Future}

class MailComponent @Inject() (mailerClient: MailerClient)(implicit ex: ExecutionContext) {

  def sendEmail(mail: models.mail.DefaultEmail) = {
    println(mail)
    val email = Email(
      subject = mail.subject,
      from = System.getenv("PLAY_MAIL_USER"),
      to = Seq(mail.to),
      bodyHtml = Some(mail.content)
    )
    Future { mailerClient.send(email) }
  }

}