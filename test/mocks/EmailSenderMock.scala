package mocks

import cats.effect.IO
import javax.inject.Singleton
import utils.{Email, EmailSender}

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
