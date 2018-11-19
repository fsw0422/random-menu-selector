package src.menu
import akka.actor.{Actor, ActorLogging, ActorSystem}
import akka.pattern.pipe

object MenuRepository {
  val actorSystem = ActorSystem("MenuRepository")
}

class MenuRepository extends Actor with ActorLogging {
  implicit val executionContext = context.dispatcher

  override def receive = {
    case "findAllMenus" =>
      pipe(Database.menus).to(sender())
  }
}
