package src.menu
import java.time.LocalTime

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import akka.pattern.{ask, pipe}
import akka.util.Timeout
import src.menu.utils.EmailDescription
import src.menu.utils.EmailSender
import src.user.{User, UserAggregate}

import scala.concurrent.duration._
import scala.util.Random

object MenuAggregate {
  val actorSystem = ActorSystem("MenuAggregate")

  val menuAggregate = actorSystem.actorOf(Props[MenuAggregate])
}

class MenuAggregate extends Actor with ActorLogging {
  implicit val executionContext = context.dispatcher
  implicit val timeout = Timeout(5 seconds)

  context.system.scheduler.schedule(
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
    receiver = self,
    message = "selectByScheduler"
  )

  override def receive = {
    case "selectByRequest" =>
      pipe(selectMenu).to(sender())
    case "selectByScheduler" =>
      selectMenu
  }

  private def selectMenu = {
    (MenuRepository.menuRepository ? "findAllMenus")
      .mapTo[List[Menu]]
      .map { menus =>
        val menu = Random.shuffle(menus).head

        (UserAggregate.userAggregate ? "getAllUsers")
          .mapTo[List[User]]
          .map(users => sendEmail(menu, users))

        menu
      }
  }

  private def sendEmail(menu: Menu, users: List[User]) = {
    val emails = users.map(user => user.email).toArray
    EmailSender.send(
      "smtp.gmail.com",
      "465",
      "menuselector0501",
      "!Q2w3e4r5t6y7u",
      true,
      true,
      "menuselector0501@gmail.com",
      "utf-8",
      EmailDescription(
        emails,
        menu.name,
        menu.link,
        emails,
        emails,
        emails
      )
    )
    users.foreach(user => log.info("Sent menu {} to {}", menu.name, user.name))
  }
}
