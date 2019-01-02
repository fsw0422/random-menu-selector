package src.menu

import akka.actor.ActorSystem
import akka.stream.ThrottleMode.Shaping
import akka.stream.scaladsl.Source
import akka.stream.{
  ActorMaterializer,
  ActorMaterializerSettings,
  OverflowStrategy
}
import com.typesafe.scalalogging.LazyLogging
import javax.inject.{Inject, Singleton}
import play.api.libs.json.JsValue
import src.user.{UserView, UserViewService}
import src.{Event, EventService, EventType}
import src.utils.{Email, EmailSender}

import scala.concurrent.duration._
import scala.util.Random

@Singleton
class Aggregate @Inject()(emailSender: EmailSender,
                          eventService: EventService,
                          menuViewService: MenuViewService,
                          userViewService: UserViewService)
    extends LazyLogging {

  private implicit val actorSystem = ActorSystem("MenuAggregate")
  private implicit val executionContext = actorSystem.dispatcher
  private implicit val actorMaterializerSettings = ActorMaterializerSettings(
    actorSystem
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
    .queue[Event](5, OverflowStrategy.backpressure)
    .throttle(1, 2 seconds, 3, Shaping)
    .alsoTo(eventService.storeEvent)
    .to(menuViewService.constructView)
    .run()

  def createOrUpdateMenu(menu: JsValue) = {
    eventBus offer Event(
      `type` = EventType.MENU_PROFILE_CREATED_OR_UPDATED,
      data = menu
    )
  }

  def selectRandomMenu() = {
    for {
      menuViews <- menuViewService.findAll()
      userViews <- userViewService.findAll()
      randomMenu = if (menuViews.nonEmpty) {
        Random.shuffle(menuViews).head
      } else {
        MenuView()
      }
      queueOfferResult <- eventBus offer Event(
        `type` = EventType.RANDOM_MENU_ASKED
      )
    } yield {
      if (userViews.nonEmpty && menuViews.nonEmpty) {
        sendEmail(randomMenu, userViews)
      }
      queueOfferResult
    }
  }

  def createOrUpdateMenuViewSchema(version: JsValue) = {
    eventBus offer Event(`type` = EventType.MENU_SCHEMA_INIT, data = version)
  }

  private def sendEmail(menu: MenuView, users: Seq[UserView]) = {
    emailSender.send(
      "smtp.gmail.com",
      "465",
      "menuselector0501",
      "!Q2w3e4r5t6y7u",
      true,
      true,
      "menuselector0501@gmail.com",
      "text/html; charset=utf-8",
      Email(
        emails = users.map(user => user.email).toArray,
        subject = "Today's Menu",
        message = s"""
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
      )
    )
  }
}
