package src.menu
import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import akka.pattern.pipe

object MenuRepository {
  val actorSystem = ActorSystem("MenuRepository")

  val menuRepository = actorSystem.actorOf(Props[MenuRepository])
}

class MenuRepository extends Actor with ActorLogging {
  implicit val executionContext = context.dispatcher

  override def receive = {
    case "findAllMenus" =>
      pipe(Database.menus).to(sender())
  }
}
