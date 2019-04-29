package menu

import java.util.UUID

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, ActorMaterializerSettings, QueueOfferResult}
import auth.Auth
import cats.data.OptionT
import cats.effect.IO
import com.typesafe.config.Config
import event.{Event, EventService, EventType}
import javax.inject.{Inject, Singleton}
import play.api.libs.json.{JsValue, Json}
import user.{UserView, UserViewService}
import utils.{Email, EmailSender, ErrorResponseMessage}

import scala.util.Random

@Singleton
class Aggregate @Inject()(
  config: Config,
  emailSender: EmailSender,
  eventService: EventService,
  auth: Auth,
  menuViewService: MenuViewService,
  userViewService: UserViewService
) {

  private implicit val actorSystem = ActorSystem("MenuAggregate")
  private implicit val executionContext = actorSystem.dispatcher
  private implicit val actorMaterializerSettings = ActorMaterializerSettings(actorSystem)
  private implicit val actorMaterializer = ActorMaterializer(actorMaterializerSettings)

  val password = config.getString("write.password")

  def createOrUpdateMenu(menu: JsValue): IO[Either[String, Option[UUID]]] = {
    auth.checkPassword(menu, password) { isAuth =>
      if (!isAuth) {
        IO.pure(Left(ErrorResponseMessage.UNAUTHORIZED))
      } else {
        val targetMenuViewOpt = menu.asOpt[MenuView]
        val result = for {
          targetMenuView <- OptionT.fromOption[IO](targetMenuViewOpt)
          menuViews <- OptionT.liftF(menuViewService.findByName(targetMenuView.name))
        } yield {
          val updatedMenuView = menuViews.headOption.fold(targetMenuView) { menuView =>
            menuView.copy(
              name = targetMenuView.name,
              ingredients = targetMenuView.ingredients,
              recipe = targetMenuView.recipe,
              link = targetMenuView.link
            )
          }

          val event = Event(
            `type` = EventType.MENU_PROFILE_CREATED_OR_UPDATED,
            data = Some(Json.toJson(updatedMenuView)),
          )
          eventService.menuEventBus offer event

          Right(updatedMenuView.uuid)
        }
        result.value.map(_.getOrElse(Left(ErrorResponseMessage.NO_SUCH_IDENTITY)))
      }
    }
  }

  def deleteMenu(menuUuid: JsValue): IO[Either[String, Option[UUID]]] = {
    auth.checkPassword(menuUuid, password) { isAuth =>
      if (!isAuth) {
        IO.pure(Left(ErrorResponseMessage.UNAUTHORIZED))
      } else {
        val targetMenuUuidStrOpt = (menuUuid \ "uuid").asOpt[String]
        IO {
          Either.cond(
            targetMenuUuidStrOpt.isDefined,
            targetMenuUuidStrOpt.map { targetMenuUuidStr =>
              val menuUuid = UUID.fromString(targetMenuUuidStr)
              menuViewService.delete(menuUuid)

              val event = Event(
                `type` = EventType.MENU_PROFILE_DELETED,
                data = Some(Json.toJson(menuUuid)),
              )
              eventService.menuEventBus offer event

              menuUuid
            },
            ErrorResponseMessage.NO_SUCH_IDENTITY
          )
        }
      }
    }
  }

  def selectRandomMenu(): IO[Either[String, Option[UUID]]] = {
    val result = for {
      menuViews <- OptionT.liftF(menuViewService.findAll())
      userViews <- OptionT.liftF(userViewService.findAll())
      randomMenuView <- OptionT.fromOption[IO](Random.shuffle(menuViews).headOption)
      selectedMenuViews <- OptionT.liftF(menuViewService.findByName(randomMenuView.name))
      selectedMenu <- OptionT.fromOption[IO](selectedMenuViews.headOption)
    } yield {
      val updatedSelectedMenuView = selectedMenu.copy(
        selectedCount = selectedMenu.selectedCount.map(_ + 1)
      )

      val event = Event(
        `type` = EventType.RANDOM_MENU_ASKED,
        data = Some(Json.toJson(updatedSelectedMenuView))
      )
      eventService.menuEventBus offer event

      if (userViews.nonEmpty && menuViews.nonEmpty) {
        sendEmail(updatedSelectedMenuView, userViews).unsafeRunSync()
        Right(updatedSelectedMenuView.uuid)
      } else {
        Right(None)
      }
    }
    result.value.map(_.getOrElse(Left(ErrorResponseMessage.NO_SUCH_IDENTITY)))
  }

  def createOrUpdateMenuViewSchema(version: JsValue): IO[Either[String, QueueOfferResult]] = {
    auth.checkPassword(version, password) { isAuth =>
      if (!isAuth) {
        IO.pure(Left(ErrorResponseMessage.UNAUTHORIZED))
      } else {
        val event = Event(`type` = EventType.MENU_SCHEMA_EVOLVED, data = Some(version))
        IO.fromFuture(IO((eventService.menuEventBus offer event).map(Right(_))))
      }
    }
  }

  private def sendEmail(menu: MenuView, users: Seq[UserView]): IO[Unit] = {
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
