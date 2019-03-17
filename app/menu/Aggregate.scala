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
class Aggregate @Inject()(
  config: Config,
  emailSender: EmailSender,
  eventService: EventService,
  menuViewService: MenuViewService,
  userViewService: UserViewService
) {

  private implicit val actorSystem = ActorSystem("MenuAggregate")
  private implicit val executionContext = actorSystem.dispatcher
  private implicit val actorMaterializerSettings = ActorMaterializerSettings(actorSystem)
  private implicit val actorMaterializer = ActorMaterializer(actorMaterializerSettings)

  def createOrUpdateMenu(menu: JsValue): Future[Option[UUID]] = {
    val menuView = menu.as[MenuView]
    for {
      updatedMenuView <- menuViewService.findByName(menuView.name)
        .map { menuViews =>
          menuViews.headOption
            .fold(menuView) { menuView =>
              menuView.copy(
                name = menuView.name,
                ingredients = menuView.ingredients,
                recipe = menuView.recipe,
                link = menuView.link
              )
            }
        }
    } yield {
      val event = Event(
        `type` = EventType.MENU_PROFILE_CREATED_OR_UPDATED,
        data = Some(Json.toJson(updatedMenuView)),
      )
      eventService.menuEventBus offer event

      updatedMenuView.uuid
    }
  }

  def deleteMenu(menu: JsValue): String = {
    val menuUuidStrOpt = (menu \ "uuid").asOpt[String]
    menuUuidStrOpt
      .fold(ResponseMessage.NO_SUCH_IDENTITY) { menuUuidStr =>
        val menuUuid = UUID.fromString(menuUuidStr)
        menuViewService.delete(menuUuid)

        val event = Event(
          `type` = EventType.MENU_PROFILE_DELETED,
          data = Some(Json.toJson(menuUuid)),
        )
        eventService.menuEventBus offer event

        menuUuidStr
      }
  }

  def selectRandomMenu(): Future[Option[UUID]] = {
    for {
      menuViews <- menuViewService.findAll()
      userViews <- userViewService.findAll()
      randomMenuView = Random.shuffle(menuViews).headOption
        .fold {
          MenuView(
            name = "NONE",
            ingredients = Seq("NONE"),
            recipe = "NONE",
            link = ""
          )
        }(menuView => menuView)
      updatedRandomMenuView <- menuViewService.findByName(randomMenuView.name)
        .map { menuViews =>
          menuViews.headOption
            .fold(randomMenuView) { menuView =>
              val updatedRandomMenuView = randomMenuView.selectedCount
                .map(_ + 1)
              menuView.copy(selectedCount = updatedRandomMenuView)
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

      updatedRandomMenuView.uuid
    }
  }

  def createOrUpdateMenuViewSchema(version: JsValue): String = {
    val event = Event(`type` = EventType.MENU_SCHEMA_EVOLVED, data = Some(version))
    eventService.menuEventBus offer event
    ResponseMessage.DATABASE_EVOLUTION
  }

  private def sendEmail(menu: MenuView, users: Seq[UserView]): Unit = {
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
