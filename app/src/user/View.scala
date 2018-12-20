package src.user
import akka.stream.scaladsl.Sink

object View {

  private var db = Seq[UserView]()

  val constructView = Sink.foreach[UserEvent] {
    case UserCreatedEvent(data) =>
    //TODO: create menu
    case UserUpdatedEvent(data) =>
    //TODO: update menu
    case UserDeletedEvent(data) =>
    //TODO: delete menu
  }

  def findAll() = {
    db.toList
  }
}
