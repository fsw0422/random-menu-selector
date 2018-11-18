package src.utils.email

import java.io.{ByteArrayInputStream, ByteArrayOutputStream}
import java.util.{Date, Properties}

import javax.activation.{DataHandler, DataSource}
import javax.mail.internet.{InternetAddress, MimeBodyPart, MimeMessage, MimeMultipart}
import javax.mail.{Address, Message, Session, Transport}

object EmailSender {

  def send(smtpHost: String,
           smtpPort: String,
           smtpUsername: String,
           smtpPassword: String,
           verifySSLCertificate: Boolean,
           sslConnection: Boolean,
           from: String,
           encoding: String,
           emailDescription: EmailDescription,
           attachmentDescriptions: AttachmentDescription*) {
    val props = setupSmtpServerProperties(
      sslConnection,
      smtpHost,
      smtpPort,
      verifySSLCertificate
    )

    val session = Session.getInstance(props)

    val m = new MimeMessage(session)
    m.setFrom(new InternetAddress(from))

    val to = convertStringEmailsToAddresses(emailDescription.emails)
    m.setRecipients(Message.RecipientType.TO, to)

    if (emailDescription.replyToEmails.nonEmpty) {
      val replyTo = convertStringEmailsToAddresses(
        emailDescription.replyToEmails
      )
      m.setReplyTo(replyTo)
    }

    if (emailDescription.ccEmails.nonEmpty) {
      val cc = convertStringEmailsToAddresses(emailDescription.ccEmails)
      m.setRecipients(Message.RecipientType.CC, cc)
    }

    if (emailDescription.bccEmails.nonEmpty) {
      val bcc = convertStringEmailsToAddresses(emailDescription.bccEmails)
      m.setRecipients(Message.RecipientType.BCC, bcc)
    }

    m.setSubject(emailDescription.subject, encoding)
    m.setSentDate(new Date())

    if (attachmentDescriptions.nonEmpty) {
      addAttachments(
        m,
        emailDescription.message,
        encoding,
        attachmentDescriptions: _*
      )
    } else {
      m.setText(emailDescription.message, encoding, "plain")
    }

    val transport = createSmtpTransportFrom(session, sslConnection)
    try {
      connectToSmtpServer(transport, smtpUsername, smtpPassword)
      sendEmail(transport, m, emailDescription, to)
    } finally {
      transport.close()
    }
  }

  private def setupSmtpServerProperties(sslConnection: Boolean,
                                        smtpHost: String,
                                        smtpPort: String,
                                        verifySSLCertificate: Boolean) = {
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
    props
  }

  private def createSmtpTransportFrom(session: Session,
                                      sslConnection: Boolean) = {
    if (sslConnection) {
      session.getTransport("smtps")
    } else {
      session.getTransport("smtp")
    }
  }

  private def sendEmail(transport: Transport,
                        m: MimeMessage,
                        emailDescription: EmailDescription,
                        to: Array[Address]) {
    transport.sendMessage(m, m.getAllRecipients)
  }

  private def connectToSmtpServer(transport: Transport,
                                  smtpUsername: String,
                                  smtpPassword: String) {
    if (smtpUsername != null && smtpUsername.nonEmpty) {
      transport.connect(smtpUsername, smtpPassword)
    } else {
      transport.connect()
    }
  }

  private def convertStringEmailsToAddresses(emails: Array[String]) = {
    val addresses = new Array[Address](emails.length)
    for (i <- emails.indices) {
      addresses(i) = new InternetAddress(emails(i))
    }
    addresses
  }

  private def addAttachments(mimeMessage: MimeMessage,
                             msg: String,
                             encoding: String,
                             attachmentDescriptions: AttachmentDescription*) {
    val multiPart = new MimeMultipart()
    val textPart = new MimeBodyPart()
    multiPart.addBodyPart(textPart)
    textPart.setText(msg, encoding, "plain")

    for (attachmentDescription <- attachmentDescriptions) {
      val binaryPart = new MimeBodyPart()
      multiPart.addBodyPart(binaryPart)

      val ds = new DataSource() {
        override def getInputStream = {
          new ByteArrayInputStream(attachmentDescription.content)
        }

        override def getOutputStream = {
          val byteStream = new ByteArrayOutputStream()
          byteStream.write(attachmentDescription.content)
          byteStream
        }

        override def getContentType = {
          attachmentDescription.contentType
        }

        override def getName = {
          attachmentDescription.filename
        }
      }

      binaryPart.setDataHandler(new DataHandler(ds))
      binaryPart.setFileName(attachmentDescription.filename)
      binaryPart.setDescription(attachmentDescription.description)
    }

    mimeMessage.setContent(multiPart)
  }
}
