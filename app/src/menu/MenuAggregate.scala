package src.menu
import java.time.LocalTime

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import akka.pattern.{ask, pipe}
import akka.stream.{ActorMaterializer, ActorMaterializerSettings}
import akka.util.Timeout

import scala.concurrent.duration._
import scala.util.Random

object MenuAggregate {
  val actorSystem = ActorSystem("MenuAggregate")
  val materializer =
    ActorMaterializer(ActorMaterializerSettings(actorSystem))(actorSystem)
  implicit val executionContext = actorSystem.dispatcher

  val selector = actorSystem.actorOf(Props[Selector])
  val emailer = actorSystem.actorOf(Props[Emailer])

  actorSystem.scheduler.schedule(
    initialDelay = {
      val time = LocalTime.of(13, 0).toSecondOfDay
      val now = LocalTime.now().toSecondOfDay
      val difference = time - now
      if (difference < 0) {
        val fullDay = 60 * 60 * 24
        fullDay + difference
      } else {
        difference
      }
    } seconds,
    interval = 24 hours,
    receiver = selector,
    message = "selectByScheduler"
  )
}

class Selector extends Actor with ActorLogging {
  implicit val executionContext = context.dispatcher
  implicit val timeout = Timeout(5 seconds)

  override def receive = {
    case "selectByRequest" =>
      val randomMenu = select.map(menus => Random.shuffle(menus).head)
      pipe(randomMenu).to(sender())
      pipe(randomMenu).to(MenuAggregate.emailer)
    case "selectByScheduler" =>
      select.map(menus => menus.head)(context.dispatcher)
  }

  def select = {
    (MenuRepository.menuRepository ? "findAllMenus").mapTo[List[Menu]]
  }
}

class Emailer extends Actor with ActorLogging {

  override def receive = {
    case Menu(typ, name, ingredients, recipe, link) => log.info("Sent email")
  }
}
