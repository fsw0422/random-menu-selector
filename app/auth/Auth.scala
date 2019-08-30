package auth

import cats.effect.IO
import com.typesafe.config.Config
import javax.inject.{Inject, Singleton}
import play.api.libs.json.{JsObject, JsValue, Json}

@Singleton
class Auth @Inject()(config: Config) {

  private val writePassword = config.getString("write.password")

  def authenticate[R](body: JsValue, password: String = writePassword)
    (accessDenied: => IO[Either[String, R]])
    (accessGranted: JsValue => IO[Either[String, R]]): IO[Either[String, R]] = {
    val attemptedPasswordOpt = (body \ "password").asOpt[String]
    attemptedPasswordOpt.fold(accessDenied) { attemptedPassword =>
      if (attemptedPassword == password) {
        accessGranted(Json.toJson(body.as[JsObject] - "password"))
      } else {
        accessDenied
      }
    }
  }
}
