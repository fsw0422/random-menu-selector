package src.user

import java.util.UUID

import play.api.libs.json.Json

object UserView {
  implicit val jsonFormat = Json.format[UserView]
}

case class UserView(userId: UUID, email: String)
