package utils

import java.util.{Date, Properties}

import cats.effect.IO
import com.google.inject.AbstractModule
import javax.inject.Singleton
import javax.mail.internet.{InternetAddress, MimeMessage}
import javax.mail.{Address, Message, Session}

final case class Email(emails: Array[String], subject: String, message: String)

trait EmailSender {

  def send(
    smtpHost: String,
    smtpPort: String,
    smtpUsername: String,
    smtpPassword: String,
    from: String,
    encoding: String,
    emailDescription: Email
  ): IO[Unit]
}

@Singleton
class EmailSenderImpl extends EmailSender {

  override def send(
    smtpHost: String,
    smtpPort: String,
    smtpUsername: String,
    smtpPassword: String,
    from: String,
    encoding: String,
    emailDescription: Email
  ): IO[Unit] = IO {
    val props = new Properties()
    props.put("mail.smtps.host", smtpHost)
    props.put("mail.smtps.port", smtpPort)
    props.put("mail.smtps.starttls.enable", "true")
    props.put("mail.smtps.ssl.checkserveridentity", "false")
    props.put("mail.smtps.ssl.trust", "*")

    val session = Session.getInstance(props)

    val message = new MimeMessage(session)
    message.setFrom(new InternetAddress(from))
    val to = emailDescription.emails.map { email =>
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

@Singleton
class EmailSenderMock extends EmailSender {

  override def send(
    smtpHost: String,
    smtpPort: String,
    smtpUsername: String,
    smtpPassword: String,
    from: String,
    encoding: String,
    emailDescription: Email
  ): IO[Unit] = IO.pure(())
}

class EmailSenderModule extends AbstractModule {

  override def configure(): Unit = {
    bind(classOf[EmailSender]).to(classOf[EmailSenderImpl])
  }
}
