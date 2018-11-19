package src.menu
import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props}
import akka.stream.ThrottleMode.Shaping
import akka.stream.scaladsl.{Sink, Source}
import akka.stream.{
  ActorMaterializer,
  ActorMaterializerSettings,
  OverflowStrategy
}
import akka.util.Timeout
import src.user.{User, UserAggregate}
import src.utils.email.{EmailDescription, EmailSender}

import scala.concurrent.duration._
import scala.util.Random

object MenuAggregate {
  val actorSystem = ActorSystem("MenuAggregate")
}

class MenuAggregate extends Actor with ActorLogging {
  implicit val executionContext = context.dispatcher
  implicit val actorMaterializer =
    ActorMaterializer(ActorMaterializerSettings(context.system))(context.system)
  implicit val timeout = Timeout(5 seconds)

  var requester: ActorRef = sender()

  override def receive = {
    case cmd: String =>
      cmd match {
        case "selectMenu" =>
          requester = sender()
          selectMenu
      }
    case menu: Menu =>
      emailAllUsers(menu)
      requester ! menu
  }

  private def menuSelector =
    Source
      .queue[String](5, OverflowStrategy.backpressure)
      .throttle(1, 2 seconds, 3, Shaping)
      .ask[List[Menu]](5)(
        MenuRepository.actorSystem.actorOf(Props[MenuRepository])
      )
      .to(Sink.foreach(menus => self forward Random.shuffle(menus).head))
      .run()

  private def selectMenu = {
    //TODO: log metrics
    menuSelector offer "findAllMenus"
  }

  private def emailer(menu: Menu) =
    Source
      .queue[String](5, OverflowStrategy.backpressure)
      .throttle(1, 2 seconds, 3, Shaping)
      .ask[List[User]](MenuRepository.actorSystem.actorOf(Props[UserAggregate]))
      .to(Sink.foreach(users => sendEmail(menu, users)))
      .run()

  private def emailAllUsers(menu: Menu) = {
    //TODO: log metrics
    emailer(menu) offer "getAllUsers"
  }

  private def sendEmail(menu: Menu, users: List[User]) = {
    val to = users.map(user => user.email).toArray
    EmailSender.send(
      "smtp.gmail.com",
      "465",
      "menuselector0501",
      "!Q2w3e4r5t6y7u",
      true,
      true,
      "menuselector0501@gmail.com",
      "text/html; charset=utf-8",
      EmailDescription(
        to,
        "Today's Menu",
        s"""
        <html>
          <head>
            <title>Today's Menu</title>
          </head>
          <body>
            <b>Ingredients</b>
            <p>
          ${menu.ingredients
          .foldLeft[String]("")((s, ingredient) => s + "<br>" + ingredient)}
            </p>
            <br>

            <b>Recipe</b>
            <p>
          ${menu.recipe
          .foldLeft[String]("")((s, instruction) => s + "<br>" + instruction)}
            </p>
            <br>

            <b>Link</b>
            <p>
            <a href=${menu.link}>${menu.name}</a>
            </p>
          </body>
        </html>
          """,
        Array(),
        Array(),
        Array()
      )
    )
  }
}
