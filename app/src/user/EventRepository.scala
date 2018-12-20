package src.user

import akka.stream.scaladsl.Sink

object EventRepository {

  val storeEvent = Sink.foreach[UserEvent] {
    case UserCreatedEvent(data) =>
    case UserUpdatedEvent(data) =>
    case UserDeletedEvent(data) =>
  }
}
