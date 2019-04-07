package auth

import cats.effect.IO
import javax.inject.Singleton
import play.api.libs.json.JsValue

import scala.concurrent.ExecutionContext

@Singleton
class Auth {

  def checkPassword[R](requestBody: JsValue, password: String)
    (action: Boolean => IO[Either[String, R]])
    (implicit executionContext: ExecutionContext): IO[Either[String, R]] = {
    val attemptedPasswordOpt = (requestBody \ "password").asOpt[String]
    val isAuth = attemptedPasswordOpt
      .fold(false)(attemptedPassword => attemptedPassword == password)
    action.apply(isAuth)
  }
}
