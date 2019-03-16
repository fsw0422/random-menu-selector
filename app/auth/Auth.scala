package auth

import javax.inject.{Inject, Singleton}
import play.api.Configuration
import play.api.libs.json.JsValue

import scala.concurrent.ExecutionContext

@Singleton
class Auth @Inject()(configuration: Configuration)(implicit executionContext: ExecutionContext) {

  def checkPassword(requestBody: JsValue): Boolean = {
    val passwordOpt = (requestBody \ "password").asOpt[String]
    passwordOpt
      .fold(false) { password =>
        password == configuration.get[String]("write.password")
      }
  }
}
