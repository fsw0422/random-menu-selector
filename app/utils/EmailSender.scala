package utils

import java.util.Date

import cats.effect.IO
import javax.inject.{Inject, Singleton}
import javax.mail.internet.{InternetAddress, MimeMessage}
import javax.mail.{Address, Message, Session, Transport}

final case class Email(
  recipients: Array[String],
  subject: String,
  message: String
)

@Singleton
class EmailSender @Inject()
(session: Session, transport: Transport) {

  private val HTML_UTF8_ENCODING = "text/html; charset=utf-8"

  def sendSMTP(
    smtpUsername: String,
    smtpPassword: String,
    emailDescription: Email
  ): IO[Unit] = IO {
    val message = new MimeMessage(session)
    val recipients = emailDescription.recipients.map { email =>
      val address: Address = new InternetAddress(email)
      address
    }
    message.setRecipients(Message.RecipientType.TO, recipients)
    message.setSentDate(new Date())
    message.setSubject(emailDescription.subject, HTML_UTF8_ENCODING)
    message.setContent(emailDescription.message, HTML_UTF8_ENCODING)

    try {
      transport.connect(smtpUsername, smtpPassword)
      transport.sendMessage(message, message.getAllRecipients)
    } finally {
      transport.close()
    }
  }
}
