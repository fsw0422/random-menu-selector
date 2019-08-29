package menu

import java.util.UUID

import auth.Auth
import cats.data.{OptionT, State}
import cats.effect.IO
import com.typesafe.config.Config
import event.{Event, EventDao, EventType}
import javax.inject.{Inject, Singleton}
import play.api.libs.json.{JsValue, Json}
import user.{User, UserViewDao}
import utils.{Email, EmailProperty, EmailSender, ErrorResponseMessage}

import scala.util.Random

final case class Menu(
  uuid: Option[UUID] = Some(UUID.randomUUID()),
  name: String,
  ingredients: Seq[String],
  recipe: String,
  link: String,
  selectedCount: Option[Int] = Some(0)
)

object Menu {

  implicit val jsonFormatter = Json
    .using[Json.WithDefaultValues]
    .format[Menu]
}

@Singleton
class Aggregate @Inject()(
  config: Config,
  emailSender: EmailSender,
  auth: Auth,
  eventDao: EventDao,
  menuViewDao: MenuViewDao,
  userViewDao: UserViewDao
) {

  private val writePassword = config.getString("write.password")
  private val emailUser = config.getString("email.user")
  private val emailPassword = config.getString("email.password")

  def createOrUpdateMenu(menu: JsValue): IO[Either[String, Int]] = {
    auth.authenticate(menu, writePassword)(IO.pure(Left(ErrorResponseMessage.UNAUTHORIZED))) {
      val result = for {
        res <- OptionT.liftF {
          val event = Event(
            `type` = EventType.MENU_PROFILE_CREATED_OR_UPDATED,
            data = Some(menu),
          )
          eventDao.insert(event)
        }
        // TODO: from here should be updated by an event as separate module
        newMenu <- OptionT.fromOption[IO](menu.asOpt[Menu])
        oldMenus <- OptionT.liftF(menuViewDao.findByName(newMenu.name))
        updatedMenu = oldMenus.headOption.fold(newMenu)(oldMenu => update(oldMenu, newMenu))
        _ <- OptionT.liftF(menuViewDao.upsert(updatedMenu))
      } yield Right(res)
      result.value.map(_.getOrElse(Left(ErrorResponseMessage.NO_SUCH_IDENTITY)))
    }
  }

  def deleteMenu(menuUuid: JsValue): IO[Either[String, Int]] = {
    auth.authenticate(menuUuid, writePassword)(IO.pure(Left(ErrorResponseMessage.UNAUTHORIZED))) {
      val result = for {
        res <- OptionT.liftF {
          val event = Event(
            `type` = EventType.MENU_PROFILE_DELETED,
            data = Some(menuUuid),
          )
          eventDao.insert(event)
        }
        // TODO: from here should be updated by an event as separate module
        targetMenuUuid <- OptionT.fromOption[IO]((menuUuid \ "uuid").asOpt[String].map(UUID.fromString))
        _ <- OptionT.liftF(menuViewDao.delete(targetMenuUuid))
      } yield Right(res)
      result.value.map(_.getOrElse(Left(ErrorResponseMessage.NO_SUCH_IDENTITY)))
    }
  }

  def selectRandomMenu(): IO[Either[String, Int]] = {
    val result = for {
      menus <- OptionT.liftF(menuViewDao.findAll())
      userViews <- OptionT.liftF(userViewDao.findAll())
      randomMenuView <- OptionT.fromOption[IO](Random.shuffle(menus).headOption)
      selectedMenuViews <- OptionT.liftF(menuViewDao.findByName(randomMenuView.name))
      selectedMenu <- OptionT.fromOption[IO](selectedMenuViews.headOption)
      updatedSelectedMenu = incrementSelectedCount(selectedMenu)
      res <- OptionT.liftF {
        val event = Event(
          `type` = EventType.RANDOM_MENU_ASKED,
          data = Some(Json.toJson(updatedSelectedMenu))
        )
        eventDao.insert(event)
      }
      // TODO: from here should be updated by an event as separate module
      _ <- OptionT.liftF(menuViewDao.upsert(updatedSelectedMenu))
      _ <- OptionT.liftF(sendMenusToAllUsers(updatedSelectedMenu, userViews))
    } yield Right(res)
    result.value.map(_.getOrElse(Left(ErrorResponseMessage.NO_SUCH_IDENTITY)))
  }

  //TODO: try to find a way to compose rather than running
  private def update(initialMenuView: Menu, menu: Menu): Menu = {
    val updatedState = State[Menu, Unit] { oldMenuView =>
      val newMenuView = oldMenuView.copy(
        name = menu.name,
        ingredients = menu.ingredients,
        recipe = menu.recipe,
        link = menu.link
      )
      (newMenuView, ())
    }
    val (updated, void) = updatedState.run(initialMenuView).value
    updated
  }

  //TODO: try to find a way to compose rather than running
  private def incrementSelectedCount(initialMenuView: Menu): Menu = {
    val selectedCountIncrementedState = State[Menu, Unit] { oldMenuView =>
      val newMenuView = oldMenuView.copy(selectedCount = oldMenuView.selectedCount.map(_ + 1))
      (newMenuView, ())
    }
    val (selectedCountIncremented, void) = selectedCountIncrementedState.run(initialMenuView).value
    selectedCountIncremented
  }

  private def sendMenusToAllUsers(menu: Menu, users: Seq[User]): IO[Unit] = {
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
