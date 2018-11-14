package src.menu
import akka.actor.{Actor, ActorSystem, Props}

import scala.util.Random

object MenuRepository {
  val actorSystem = ActorSystem("MenuRepository")

  val menuRepository = actorSystem.actorOf(Props[MenuRepository])
}

class MenuRepository extends Actor {
  val menuList = Random.shuffle(
    List(
      Menu("Asian", "Noodle", List("Green Onion")),
      Menu("American", "Popcorn", List("corns"))
    )
  )

  override def receive = {
    case "findAllMenus" =>
      sender() ! menuList
  }
}
