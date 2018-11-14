package src.menu
import java.time.LocalTime

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import akka.pattern.ask
import akka.stream.{ActorMaterializer, ActorMaterializerSettings}
import akka.util.Timeout

import scala.concurrent.duration._

object MenuAggregate {
  val actorSystem = ActorSystem("MenuAggregate")
  val materializer =
    ActorMaterializer(ActorMaterializerSettings(actorSystem))(actorSystem)

  val selector = actorSystem.actorOf(Props[Selector])

  actorSystem.scheduler.schedule(
    initialDelay = {
      val time = LocalTime.of(13, 0).toSecondOfDay
      val now = LocalTime.now().toSecondOfDay
      val fullDay = 60 * 60 * 24
      val difference = time - now
      if (difference < 0) {
        fullDay + difference
      } else {
        time - now
      }
    } seconds,
    interval = 24 hours,
    receiver = selector,
    message = "selectByScheduler"
  )(actorSystem.dispatcher)
}

class Selector extends Actor with ActorLogging {
  //TODO: change to StateT Monad
  var currentMenu: Option[Menu] = None

  override def receive = {
    case "selectByRequest" =>
      select.map { menus =>
        currentMenu = Option(menus.head)
        log.info("{}", currentMenu.get.name)
        sender() ! currentMenu.get
      }(context.dispatcher)
    case "selectByScheduler" =>
      select.map(menus => currentMenu = Option(menus.head))(context.dispatcher)
  }

  def select = {
    implicit val timeout = Timeout(5 seconds)
    (MenuRepository.menuRepository ? "findAllMenus").mapTo[List[Menu]]
  }
}
