package src.menu

import akka.actor.ActorSystem
import akka.http.scaladsl.model.DateTime
import akka.stream.ThrottleMode.Shaping
import akka.stream.scaladsl.Source
import akka.stream.{ActorMaterializer, ActorMaterializerSettings, OverflowStrategy}
import src.user.{UserView, UserViewService}
import src.utils.email.{EmailDescription, EmailSender}

import scala.concurrent.duration._
import scala.util.Random

object Aggregate {

  private implicit val executionContext = ActorSystem("Menu")
  private implicit val actorMaterializerSettings = ActorMaterializerSettings(
    executionContext
  )
  private implicit val actorMaterializer = ActorMaterializer(
    actorMaterializerSettings
  )

  /*
   * Event.scala bus stream that acts as a broker between aggregate and
   * - Event.scala Store which stores the events
   * - View component that constructs / persists view models
   */
  private val eventBus = Source
    .queue[MenuEvent](5, OverflowStrategy.backpressure)
    .throttle(1, 2 seconds, 3, Shaping)
    .alsoTo(MenuEventService.storeEvent)
    .to(MenuViewService.constructView)
    .run()

  def selectRandomMenu() = {
    val randomMenu = Random.shuffle(MenuViewService.findAll()).head
    val users = UserViewService.findAll()
    sendEmail(randomMenu, users)
    eventBus offer MenuEvent(None, Option(DateTime.now.clicks), "RandomMenuSelectedEvent", "data")
  }

  private def sendEmail(menu: MenuView, users: List[UserView]) = {
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
        users.map(user => user.email).toArray,
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
