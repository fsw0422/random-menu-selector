package menu

import java.util.UUID

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, ActorMaterializerSettings}
import com.typesafe.config.Config
import event.{Event, EventService, EventType}
import javax.inject.{Inject, Singleton}
import play.api.libs.json.{JsValue, Json}
import user.{UserView, UserViewService}
import utils.{Email, EmailSender, ResponseMessage}

import scala.concurrent.Future
import scala.util.Random

@Singleton
class Aggregate @Inject()(config: Config,
                          emailSender: EmailSender,
                          eventService: EventService,
                          menuViewService: MenuViewService,
                          userViewService: UserViewService) {

  private implicit val actorSystem = ActorSystem("MenuAggregate")
  private implicit val executionContext = actorSystem.dispatcher
  private implicit val actorMaterializerSettings = ActorMaterializerSettings(
    actorSystem
  )
  private implicit val actorMaterializer = ActorMaterializer(
    actorMaterializerSettings
  )

  def createOrUpdateMenu(menu: JsValue) = {
    val menuView = menu.as[MenuView]
    for {
      updatedMenuView <- menuViewService
        .findByName(menuView.name)
        .map { menuViews =>
          if (menuViews.nonEmpty) {
            menuViews.head.copy(
              name = menuView.name,
              ingredients = menuView.ingredients,
              recipe = menuView.recipe,
              link = menuView.link
            )
          } else {
            menuView
          }
        }
    } yield {
      val event = Event(
        `type` = EventType.MENU_PROFILE_CREATED_OR_UPDATED,
        data = Some(Json.toJson(updatedMenuView)),
      )
      eventService.menuEventBus offer event

      updatedMenuView.uuid.get
    }
  }

  def deleteMenu(menu: JsValue) = {
    val menuUuidOption = (menu \ "uuid").asOpt[String]
    if (menuUuidOption.isEmpty) {
      Future(ResponseMessage.NO_SUCH_IDENTITY)
    } else {
      val menuUuid = UUID.fromString(menuUuidOption.get)
      menuViewService.delete(menuUuid)

      val event = Event(
        `type` = EventType.MENU_PROFILE_DELETED,
        data = Some(Json.toJson(menuUuid)),
      )
      eventService.menuEventBus offer event

      Future(menuUuid)
    }
  }

  def selectRandomMenu() = {
    for {
      menuViews <- menuViewService.findAll()
      userViews <- userViewService.findAll()
      randomMenuView = if (menuViews.nonEmpty) {
        Random.shuffle(menuViews).head
      } else {
        MenuView(
          name = "NONE",
          ingredients = Seq("NONE"),
          recipe = "NONE",
          link = ""
        )
      }
      updatedRandomMenuView <- menuViewService
        .findByName(randomMenuView.name)
        .map { menuViews =>
          if (menuViews.nonEmpty) {
            menuViews.head.copy(
              selectedCount = randomMenuView.selectedCount.map(_ + 1)
            )
          } else {
            randomMenuView
          }
        }
    } yield {
      val event = Event(
        `type` = EventType.RANDOM_MENU_ASKED,
        data = Some(Json.toJson(updatedRandomMenuView))
      )
      eventService.menuEventBus offer event

      if (userViews.nonEmpty && menuViews.nonEmpty) {
        sendEmail(randomMenuView, userViews)
      }

      updatedRandomMenuView.uuid.get
    }
  }

  def createOrUpdateMenuViewSchema(version: JsValue) = {
    val event =
      Event(`type` = EventType.MENU_SCHEMA_EVOLVED, data = Some(version))
    eventService.menuEventBus offer event
    Future(ResponseMessage.DATABASE_EVOLUTION)
  }

  private def sendEmail(menu: MenuView, users: Seq[UserView]) = {
    emailSender.send(
      "smtp.gmail.com",
      "465",
      "menuselector0501",
      config.getString("email.password"),
      "menuselector0501@gmail.com",
      "text/html; charset=utf-8",
      Email(
        emails = users.map(user => user.email).toArray,
        subject = "Today's Menu",
        message = s"""
        <html>
          <head>
          </head>
          <body>

            <b>Menu</b>
            <p>
          ${menu.name}
            </p>
            <br>

            <b>Ingredients</b>
            <p>
          ${menu.ingredients.mkString("<br>")}
            </p>
            <br>

            <b>Recipe</b>
            <p>
          ${menu.recipe}
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
