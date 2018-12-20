package src.user

import play.api.libs.json.JsValue

sealed trait UserEvent

case class UserCreatedEvent(data: JsValue) extends UserEvent

case class UserUpdatedEvent(data: JsValue) extends UserEvent

case class UserDeletedEvent(data: JsValue) extends UserEvent
