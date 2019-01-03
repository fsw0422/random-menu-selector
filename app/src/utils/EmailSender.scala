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
           verifySSLCertificate: Boolean,
           sslConnection: Boolean,
           from: String,
           encoding: String,
           emailDescription: Email) {
    val props = new Properties()
    if (sslConnection) {
      props.put("mail.smtps.host", smtpHost)
      props.put("mail.smtps.port", smtpPort)
      props.put("mail.smtps.starttls.enable", "true")
      if (!verifySSLCertificate) {
        props.put("mail.smtps.ssl.checkserveridentity", "false")
        props.put("mail.smtps.ssl.trust", "*")
      }
    } else {
      props.put("mail.smtp.host", smtpHost)
      props.put("mail.smtp.port", smtpPort)
    }

    val session = Session.getInstance(props)

    val m = new MimeMessage(session)
    m.setFrom(new InternetAddress(from))

    val to = convertStringEmailsToAddresses(emailDescription.emails)
    m.setRecipients(Message.RecipientType.TO, to)
    m.setSubject(emailDescription.subject, encoding)
    m.setSentDate(new Date())
    m.setContent(emailDescription.message, encoding)

    val transport = if (sslConnection) {
      session.getTransport("smtps")
    } else {
      session.getTransport("smtp")
    }
    try {
      if (smtpUsername != null && smtpUsername.nonEmpty) {
        transport.connect(smtpUsername, smtpPassword)
      } else {
        transport.connect()
      }
      transport.sendMessage(m, m.getAllRecipients)
    } finally {
      transport.close()
    }
  }

  private def convertStringEmailsToAddresses(emails: Array[String]) = {
    val addresses = new Array[Address](emails.length)
    for (i <- emails.indices) {
      addresses(i) = new InternetAddress(emails(i))
    }
    addresses
  }
}
