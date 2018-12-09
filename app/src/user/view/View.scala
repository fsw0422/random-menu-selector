package src.user.view

import akka.stream.scaladsl.Sink
import src.user.event.{UserCreatedEvent, UserDeletedEvent, UserEvent, UserUpdatedEvent}

object View {

  private var db = Seq[UserView]()

  val constructView = Sink.foreach[UserEvent] {
    case UserCreatedEvent(eventId, timeStamp, menu) =>
      println(eventId)
    //TODO: create menu
    case UserUpdatedEvent(eventId, timeStamp, menu) =>
      println(eventId)
    //TODO: update menu
    case UserDeletedEvent(eventId, timeStamp, menuId) =>
      println(eventId)
    //TODO: delete menu
  }

  def findAll() = {
    db.toList
  }
}
