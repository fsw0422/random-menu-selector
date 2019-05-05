package auth

import cats.effect.IO
import javax.inject.Singleton
import play.api.libs.json.JsValue

@Singleton
class Auth {

  def fold[R](requestBody: JsValue, password: String)
    (accessDenied: => IO[Either[String, R]])
    (accessGranted: => IO[Either[String, R]]): IO[Either[String, R]] = {
    val attemptedPasswordOpt = (requestBody \ "password").asOpt[String]
    attemptedPasswordOpt.fold(accessDenied) { attemptedPassword =>
      if (attemptedPassword == password) {
        accessGranted
      } else {
        accessDenied
      }
    }
  }
}
