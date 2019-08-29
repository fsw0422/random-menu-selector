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
import utils.{Email, EmailProperty, EmailSender, ResponseMessage}

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

  def createOrUpdateMenu(menu: JsValue): IO[Either[String, String]] = {
    auth.authenticate(menu, writePassword)(IO.pure(Left(ResponseMessage.UNAUTHORIZED))) {
      val result = for {
        response <- OptionT.liftF {
          val event = Event(
            `type` = EventType.MENU_CREATED_OR_UPDATED,
            data = Some(menu),
          )
          eventDao.insert(event)
        }
        menu <- OptionT.fromOption[IO](menu.asOpt[Menu])
        _ <- OptionT.liftF(menuViewDao.upsert(menu))
      } yield  {
        response match {
          case 1 => Right(ResponseMessage.SUCCESS)
          case _ => Left(ResponseMessage.FAILED)
        }
      }
      result.value.map(_.getOrElse(Left(ResponseMessage.NO_SUCH_IDENTITY)))
    }
  }

  def deleteMenu(menuUuid: JsValue): IO[Either[String, String]] = {
    auth.authenticate(menuUuid, writePassword)(IO.pure(Left(ResponseMessage.UNAUTHORIZED))) {
      val result = for {
        response <- OptionT.liftF {
          val event = Event(
            `type` = EventType.MENU_PROFILE_DELETED,
            data = Some(menuUuid),
          )
          eventDao.insert(event)
        }
        targetMenuUuid <- OptionT.fromOption[IO]((menuUuid \ "uuid").asOpt[String].map(UUID.fromString))
        _ <- OptionT.liftF(menuViewDao.delete(targetMenuUuid))
      } yield {
        response match {
          case 1 => Right(ResponseMessage.SUCCESS)
          case _ => Left(ResponseMessage.FAILED)
        }
      }
      result.value.map(_.getOrElse(Left(ResponseMessage.NO_SUCH_IDENTITY)))
    }
  }

  /*
   * TODO: Change this API to accept menu UUID
   *       This means that shuffling should be delegated to caller
   *       Aggregate should not refer to previous state by querying a view
   */
  def selectRandomMenu(): IO[Either[String, String]] = {
    val result = for {
      menus <- OptionT.liftF(menuViewDao.findAll())
      selectedMenu <- OptionT.fromOption[IO](Random.shuffle(menus).headOption)

      updatedSelectedMenu = incrementSelectedCount(selectedMenu)
      response <- OptionT.liftF {
        val event = Event(
          `type` = EventType.MENU_SELECTED,
          data = Some(Json.toJson(updatedSelectedMenu))
        )
        eventDao.insert(event)
      }
      _ <- OptionT.liftF(menuViewDao.upsert(updatedSelectedMenu))
      userViews <- OptionT.liftF(userViewDao.findAll())
      _ <- OptionT.liftF(sendMenusToAllUsers(updatedSelectedMenu, userViews))
    } yield {
      response match {
        case 1 => Right(ResponseMessage.SUCCESS)
        case _ => Left(ResponseMessage.FAILED)
      }
    }
    result.value.map(_.getOrElse(Left(ResponseMessage.NO_SUCH_IDENTITY)))
  }

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
