package src.menu
import akka.actor.Actor

class MenuAggregate extends Actor {

  override def receive = {
    case "Western" =>
      sender() ! Menu("", "Cake", Seq("Orange", "Apple"))
    case "Eastern" =>
      sender() ! Menu("", "Noodles", Seq("Orange", "Apple"))
  }
}
