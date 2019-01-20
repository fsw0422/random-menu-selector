package auth

import javax.inject.{Inject, Singleton}
import play.api.Configuration
import play.api.libs.json.JsValue

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class Auth @Inject()(configuration: Configuration)(
  implicit executionContext: ExecutionContext
) {

  def checkPassword(requestBody: JsValue) = {
    val password = (requestBody \ "password").asOpt[String]
    val isAuth = if (password.isEmpty) {
      false
    } else {
      password.map { password =>
        password == configuration.get[String]("write.password")
      }.get
    }
    Future(isAuth)
  }
}
