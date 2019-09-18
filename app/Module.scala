import java.util.Properties

import com.google.inject.AbstractModule
import javax.mail.{Session, Transport}

class Module extends AbstractModule {

  override protected def configure(): Unit = {
    val emailSession = getEmailSession()
    bind(classOf[Session]).toInstance(emailSession)
    val emailTransport = emailSession.getTransport("smtps")
    bind(classOf[Transport]).toInstance(emailTransport)
  }

  private def getEmailSession(): Session = {
    val gmailProps = new Properties()
    gmailProps.put("mail.smtps.host", "smtp.gmail.com")
    gmailProps.put("mail.smtps.port", "465")
    gmailProps.put("mail.smtps.starttls.enable", "true")
    gmailProps.put("mail.smtps.ssl.checkserveridentity", "false")
    gmailProps.put("mail.smtps.ssl.trust", "*")
    Session.getInstance(gmailProps)
  }
}

