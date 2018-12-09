package src.user.event

import akka.stream.scaladsl.Sink

object EventRepository {

  val storeEvent = Sink.foreach[UserEvent] {
    case UserCreatedEvent(eventId, timeStamp, user) =>
    case UserUpdatedEvent(eventId, timeStamp, user) =>
    case UserDeletedEvent(eventId, timeStamp, userId) =>
  }
}
