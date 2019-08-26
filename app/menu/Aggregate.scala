package menu

import java.util.UUID

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, ActorMaterializerSettings, QueueOfferResult}
import auth.Auth
import cats.data.{OptionT, State}
import cats.effect.IO
import com.typesafe.config.Config
import event.{Event, EventService, EventType}
import javax.inject.{Inject, Singleton}
import play.api.libs.json.{JsValue, Json}
import user.{UserView, UserViewService}
import utils.{Email, EmailProperty, EmailSender, ErrorResponseMessage}

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

  private val writePassword = config.getString("write.password")
  private val emailUser = config.getString("email.user")
  private val emailPassword = config.getString("email.password")

  def createOrUpdateMenu(menu: JsValue): IO[Either[String, Option[UUID]]] = {
    auth.authenticate(menu, writePassword)(IO.pure(Left(ErrorResponseMessage.UNAUTHORIZED))) {
      val newMenuViewOpt = menu.asOpt[MenuView]
      val result = for {
        newMenuView <- OptionT.fromOption[IO](newMenuViewOpt)
        menuViews <- OptionT.liftF(menuViewService.findByName(newMenuView.name))
        updatedMenuView = menuViews.headOption.fold(newMenuView)(oldMenuView => update(oldMenuView, newMenuView))
        _ <- OptionT.liftF {
          val event = Event(
            `type` = EventType.MENU_PROFILE_CREATED_OR_UPDATED,
            data = Some(Json.toJson(updatedMenuView)),
          )
          IO.fromFuture(IO(eventService.menuEventBus offer event))
        }
      } yield Right(updatedMenuView.uuid)
      result.value.map(_.getOrElse(Left(ErrorResponseMessage.NO_SUCH_IDENTITY)))
    }
  }

  def deleteMenu(menuUuid: JsValue): IO[Either[String, Option[UUID]]] = {
    auth.authenticate(menuUuid, writePassword)(IO.pure(Left(ErrorResponseMessage.UNAUTHORIZED))) {
      val targetMenuUuidStrOpt = (menuUuid \ "uuid").asOpt[String]
      val result = for {
        targetMenuUuidStr <- OptionT.fromOption[IO](targetMenuUuidStrOpt)
        targetMenuUuid = UUID.fromString(targetMenuUuidStr)
        _ <- OptionT.liftF(menuViewService.delete(targetMenuUuid))
        _ <- OptionT.liftF {
          val event = Event(
            `type` = EventType.MENU_PROFILE_DELETED,
            data = Some(Json.toJson(targetMenuUuid)),
          )
          IO.fromFuture(IO(eventService.menuEventBus offer event))
        }
      } yield Right(targetMenuUuidStrOpt.map(UUID.fromString))
      result.value.map(_.getOrElse(Left(ErrorResponseMessage.NO_SUCH_IDENTITY)))
    }
  }

  def selectRandomMenu(): IO[Either[String, Option[UUID]]] = {
    val result = for {
      menuViews <- OptionT.liftF(menuViewService.findAll())
      userViews <- OptionT.liftF(userViewService.findAll())
      randomMenuView <- OptionT.fromOption[IO](Random.shuffle(menuViews).headOption)
      selectedMenuViews <- OptionT.liftF(menuViewService.findByName(randomMenuView.name))
      selectedMenu <- OptionT.fromOption[IO](selectedMenuViews.headOption)
      updatedSelectedMenuView = incrementSelectedCount(selectedMenu)
      _ <- OptionT.liftF(sendMenusToAllUsers(updatedSelectedMenuView, userViews))
      _ <- OptionT.liftF {
        val event = Event(
          `type` = EventType.RANDOM_MENU_ASKED,
          data = Some(Json.toJson(updatedSelectedMenuView))
        )
        IO.fromFuture(IO(eventService.menuEventBus offer event))
      }
    } yield Right(updatedSelectedMenuView.uuid)
    result.value.map(_.getOrElse(Left(ErrorResponseMessage.NO_SUCH_IDENTITY)))
  }

  def createOrUpdateMenuViewSchema(version: JsValue): IO[Either[String, QueueOfferResult]] = {
    auth.authenticate(version, writePassword)(IO.pure(Left(ErrorResponseMessage.UNAUTHORIZED))) {
      val event = Event(`type` = EventType.MENU_SCHEMA_EVOLVED, data = Some(version))
      IO.fromFuture(IO((eventService.menuEventBus offer event).map(Right(_))))
    }
  }

  private def update(initialMenuView: MenuView, menuView: MenuView): MenuView = {
    val updatedState = State[MenuView, Unit] { oldMenuView =>
      val newMenuView = oldMenuView.copy(
        name = menuView.name,
        ingredients = menuView.ingredients,
        recipe = menuView.recipe,
        link = menuView.link
      )
      (newMenuView, ())
    }
    val (updated, void) = updatedState.run(initialMenuView).value
    updated
  }

  private def incrementSelectedCount(initialMenuView: MenuView): MenuView = {
    val selectedCountIncrementedState = State[MenuView, Unit] { oldMenuView =>
      val newMenuView = oldMenuView.copy(selectedCount = oldMenuView.selectedCount.map(_ + 1))
      (newMenuView, ())
    }
    val (selectedCountIncremented, void) = selectedCountIncrementedState.run(initialMenuView).value
    selectedCountIncremented
  }

  private def sendMenusToAllUsers(menu: MenuView, users: Seq[UserView]): IO[Unit] = {
    emailSender.sendSMTP(
      emailUser,
      emailPassword,
      EmailProperty.gmailProperties,
      Email(
        recipients = users.map(user => user.email).toArray,
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
