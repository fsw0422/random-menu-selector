package src.menu
import akka.actor.Actor

class MenuRepository extends Actor {
  val menuList = Seq(Menu("Noodles", Seq("Orange")))

  override def receive = {
    case _ =>
      sender() ! menuList
  }
}
