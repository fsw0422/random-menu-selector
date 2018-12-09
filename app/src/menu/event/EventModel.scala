package src.menu.event

import java.util.UUID
import play.api.libs.json._

sealed trait MenuEvent

case class MenuCreatedEvent(eventId: UUID, timeStamp: Long, menu: JsValue)
    extends MenuEvent

case class MenuUpdatedEvent(eventId: UUID, timeStamp: Long, menu: JsValue)
    extends MenuEvent

case class MenuDeletedEvent(eventId: UUID, timeStamp: Long, menuId: UUID)
    extends MenuEvent

case class RandomMenuSelectedEvent(eventId: UUID, timeStamp: Long, menu: JsValue)
    extends MenuEvent
