package src.menu.event

import akka.stream.scaladsl.Sink

object EventRepository {

  val storeEvent = Sink.foreach[MenuEvent] {
    case MenuCreatedEvent(eventId, timeStamp, menu) =>
      // TODO: store event
    case MenuUpdatedEvent(eventId, timeStamp, menu) =>
      // TODO: store event
    case MenuDeletedEvent(eventId, timeStamp, menuId) =>
      // TODO: store event
    case RandomMenuSelectedEvent(eventId, timeStamp, menu) =>
      // TODO: store event
  }
}
