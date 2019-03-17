package auth

import javax.inject.{Inject, Singleton}
import play.api.Configuration
import play.api.libs.json.JsValue

@Singleton
class Auth {

  def checkPassword(requestBody: JsValue, password: String): Boolean = {
    val attemptedPasswordOpt = (requestBody \ "password").asOpt[String]
    attemptedPasswordOpt
      .fold(false) { attemptedPassword =>
        attemptedPassword == password
      }
  }
}
