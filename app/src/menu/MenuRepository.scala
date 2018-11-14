package src.menu
import akka.actor.{Actor, ActorLogging, ActorSystem, Props}

object MenuRepository {
  val actorSystem = ActorSystem("MenuRepository")

  val menuRepository = actorSystem.actorOf(Props[MenuRepository])

  //TODO:
  val database = List(
    Menu(
      "Asian",
      "Noodle",
      Seq("Green Onion"),
      List("put noodle in hot water", "eat"),
      "http://asian.example.com"
    ),
    Menu(
      "American",
      "Popcorn",
      Seq("corns"),
      List("put in microwave", "put butter", "eat"),
      "http://american.example.com"
    )
  )
}

class MenuRepository extends Actor with ActorLogging {

  override def receive = {
    case "findAllMenus" =>
      sender() ! MenuRepository.database
  }
}
