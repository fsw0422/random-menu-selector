package src.user.event

import java.util.UUID

import play.api.libs.json.JsValue

sealed trait UserEvent

case class UserCreatedEvent(eventId: UUID, timeStamp: Long, user: JsValue)
    extends UserEvent

case class UserUpdatedEvent(eventId: UUID, timeStamp: Long, user: JsValue)
    extends UserEvent

case class UserDeletedEvent(eventId: UUID, timeStamp: Long, userId: UUID)
    extends UserEvent
