package src.menu.view

import akka.stream.scaladsl.Sink
import src.menu.event._

object View {

  private var db = List[MenuView]()

  val constructView = Sink.foreach[MenuEvent] {
    case MenuCreatedEvent(eventId, timeStamp, menu) =>
    //TODO: create menu
    case MenuUpdatedEvent(eventId, timeStamp, menu) =>
    //TODO: update menu
    case MenuDeletedEvent(eventId, timeStamp, menuId) =>
    //TODO: delete menu
    case RandomMenuSelectedEvent(eventId, timeStamp, menu) =>
    // TODO: store event
  }

  // TODO: store in persistent
  def findAll() = {
    db
  }
}
