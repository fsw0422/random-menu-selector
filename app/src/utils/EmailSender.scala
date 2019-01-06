package src.utils

import java.util.{Date, Properties}
import javax.inject.Singleton
import javax.mail.internet.{InternetAddress, MimeMessage}
import javax.mail.{Address, Message, Session}

case class Email(emails: Array[String], subject: String, message: String)

@Singleton
class EmailSender {

  def send(smtpHost: String,
           smtpPort: String,
           smtpUsername: String,
           smtpPassword: String,
           from: String,
           encoding: String,
           emailDescription: Email) {
    val props = new Properties()
    props.put("mail.smtps.host", smtpHost)
    props.put("mail.smtps.port", smtpPort)
    props.put("mail.smtps.starttls.enable", "true")
    props.put("mail.smtps.ssl.checkserveridentity", "false")
    props.put("mail.smtps.ssl.trust", "*")

    val session = Session.getInstance(props)

    val message = new MimeMessage(session)
    message.setFrom(new InternetAddress(from))
    val to = emailDescription.emails
      .map { email =>
        val address: Address = new InternetAddress(email)
        address
      }
    message.setRecipients(Message.RecipientType.TO, to)
    message.setSentDate(new Date())
    message.setSubject(emailDescription.subject, encoding)
    message.setContent(emailDescription.message, encoding)

    val transport = session.getTransport("smtps")
    try {
      transport.connect(smtpUsername, smtpPassword)
      transport.sendMessage(message, message.getAllRecipients)
    } finally {
      transport.close()
    }
  }
}
