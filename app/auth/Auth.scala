package auth

import javax.inject.Singleton
import play.api.libs.json.JsValue

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class Auth {

  def checkPassword[R](requestBody: JsValue, password: String)
    (action: Boolean => Future[Either[String, R]])
    (implicit executionContext: ExecutionContext): Future[Either[String, R]] = {
    val attemptedPasswordOpt = (requestBody \ "password").asOpt[String]
    val isAuth = attemptedPasswordOpt
      .fold(false)(attemptedPassword => attemptedPassword == password)
    action.apply(isAuth)
  }
}
