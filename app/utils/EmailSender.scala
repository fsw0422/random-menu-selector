package utils

import java.util.{Date, Properties}

import cats.effect.IO
import javax.inject.Singleton
import javax.mail.internet.{InternetAddress, MimeMessage}
import javax.mail.{Address, Message, Session}

final case class Email(recipients: Array[String], subject: String, message: String)

@Singleton
class EmailSender {

  def sendSMTP(
    smtpUsername: String,
    smtpPassword: String,
    smtpProperties: Properties,
    emailDescription: Email
  ): IO[Unit] = IO {
    val session = Session.getInstance(smtpProperties)
    val message = new MimeMessage(session)
    val recipients = emailDescription.recipients.map { email =>
      val address: Address = new InternetAddress(email)
      address
    }
    message.setRecipients(Message.RecipientType.TO, recipients)
    message.setSentDate(new Date())
    message.setSubject(emailDescription.subject, EmailProperty.HTML_UTF8_ENCODING)
    message.setContent(emailDescription.message, EmailProperty.HTML_UTF8_ENCODING)

    val transport = session.getTransport("smtps")
    try {
      transport.connect(smtpUsername, smtpPassword)
      transport.sendMessage(message, message.getAllRecipients)
    } finally {
      transport.close()
    }
  }
}

object EmailProperty {
  val HTML_UTF8_ENCODING = "text/html; charset=utf-8"

  val gmailProperties = {
    val gmailProps = new Properties()
    gmailProps.put("mail.smtps.host", "smtp.gmail.com")
    gmailProps.put("mail.smtps.port", "465")
    gmailProps.put("mail.smtps.starttls.enable", "true")
    gmailProps.put("mail.smtps.ssl.checkserveridentity", "false")
    gmailProps.put("mail.smtps.ssl.trust", "*")
    gmailProps
  }
}
