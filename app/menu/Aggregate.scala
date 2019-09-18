package menu

import java.util.UUID

import cats.effect.IO
import com.typesafe.config.Config
import event.{Event, EventDao, EventType}
import javax.inject.{Inject, Singleton}
import play.api.libs.json.{JsObject, JsValue, Json}
import utils.{GenericToolset, ResponseMessage}

import scala.concurrent.Future

final case class Menu(
  uuid: Option[UUID],
  name: Option[String],
  ingredients: Option[Seq[String]],
  recipe: Option[String],
  link: Option[String],
  selectedCount: Option[Int],
  passwordAttempt: Option[String]
) {

  def validateRegisterParams[A](notValid: => A)(valid: Menu => A): A = {
    (this.ingredients.isDefined, this.link.isDefined, this.name.isDefined, this.recipe.isDefined) match {
      case (true, true, true, true) =>
        valid.apply(this)
      case _ =>
        notValid
    }
  }

  def validateEditParams[A](notValid: => A)(valid: Menu => A): A = {
    (this.uuid.isDefined) match {
      case true =>
        valid.apply(this)
      case _ =>
        notValid
    }
  }
}

object Menu {

  val aggregateName = "MENU"

  implicit val jsonFormatter = Json
    .using[Json.WithDefaultValues]
    .format[Menu]
}

@Singleton
class Aggregate @Inject()(
  config: Config,
  genericToolset: GenericToolset,
  eventDao: EventDao,
  viewHandler: ViewHandler
) {

  def register(menuOpt: Option[Menu]): IO[Either[String, String]] = {
    menuOpt.fold {
      val result: IO[Either[String, String]] = IO.pure(Left(ResponseMessage.PARAM_ERROR))
      result
    } { menu =>
      authenticate(menu) { menu =>
        menu.validateRegisterParams {
          val result: IO[Either[String, String]] = IO.pure(Left(ResponseMessage.PARAM_MISSING))
          result
        } { menu =>
          val newMenu = menu.copy(uuid = Some(genericToolset.randomUUID()), selectedCount = Some(0))
          val event = Event(
            uuid = Some(genericToolset.randomUUID()),
            `type` = EventType.MENU_CREATED,
            aggregate = Menu.aggregateName,
            data = Json.toJson(newMenu),
            timestamp = genericToolset.currentTime()
          )
          for {
            eventResult <- IO.fromFuture(IO(eventDao.insert(event)))
            viewResult <- viewHandler.create(newMenu)
          } yield {
            (eventResult, viewResult) match {
              case (1, 1) =>
                Right(ResponseMessage.SUCCESS)
              case _ =>
                Left(ResponseMessage.FAILED)
            }
          }
        }
      }
    }
  }

  def edit(menuOpt: Option[Menu]): IO[Either[String, String]] = {
    menuOpt.fold {
      val result: IO[Either[String, String]] = IO.pure(Left(ResponseMessage.PARAM_ERROR))
      result
    } { menu =>
      menu.validateEditParams {
        val result: IO[Either[String, String]] = IO.pure(Left(ResponseMessage.PARAM_MISSING))
        result
      } { menu =>
        authenticate(menu) { menu =>
          val event = Event(
            uuid = Some(genericToolset.randomUUID()),
            `type` = EventType.MENU_UPDATED,
            aggregate = Menu.aggregateName,
            data = Json.toJson(menu),
            timestamp = genericToolset.currentTime()
          )
          for {
            eventResult <- IO.fromFuture(IO(eventDao.insert(event)))
            viewResult <- viewHandler.update(menu)
          } yield {
            (eventResult, viewResult) match {
              case (1, 1) =>
                Right(ResponseMessage.SUCCESS)
              case _ =>
                Left(ResponseMessage.FAILED)
            }
          }
        }
      }
    }
  }

  def remove(menuOpt: Option[Menu]): IO[Either[String, String]] = {
    menuOpt.fold {
      val result: IO[Either[String, String]] = IO.pure(Left(ResponseMessage.PARAM_ERROR))
      result
    } { menu =>
      authenticate(menu) { menu =>
        val menuUuid = menu.uuid.getOrElse(UUID.fromString(""))
        val event = Event(
          uuid = Some(genericToolset.randomUUID()),
          `type` = EventType.MENU_DELETED,
          aggregate = Menu.aggregateName,
          data = Json.obj("uuid" -> Json.toJson(menuUuid)),
          timestamp = genericToolset.currentTime()
        )
        for {
          eventResult <- IO.fromFuture(IO(eventDao.insert(event)))
          viewResult <- viewHandler.delete(menuUuid)
        } yield {
          (eventResult, viewResult) match {
            case (1, 1) =>
              Right(ResponseMessage.SUCCESS)
            case _ =>
              Left(ResponseMessage.FAILED)
          }
        }
      }
    }
  }

  def selectMenu(uuidOpt: Option[UUID]): IO[Either[String, String]] = {
    uuidOpt.fold {
      val result: IO[Either[String, String]] = IO.pure(Left(ResponseMessage.PARAM_ERROR))
      result
    } { selectedUuid =>
      val latestSelectedMenuEvents = IO.fromFuture(IO(getLatestSelectedMenuEvents(selectedUuid))).unsafeRunSync()
      val newMenu = latestSelectedMenuEvents.headOption.fold {
        Menu(
          uuid = Some(selectedUuid),
          name = None,
          ingredients = None,
          recipe = None,
          link = None,
          selectedCount = Some(0),
          passwordAttempt = None
        )
      } { latestSelectedMenuEvent =>
        val latestSelectedMenu = latestSelectedMenuEvent.data.as[Menu]
        latestSelectedMenu.copy(selectedCount = latestSelectedMenu.selectedCount.map(_ + 1))
      }

      val event = Event(
        uuid = Some(genericToolset.randomUUID()),
        `type` = EventType.MENU_SELECTED,
        aggregate = Menu.aggregateName,
        data = Json.toJson(newMenu),
        timestamp = genericToolset.currentTime()
      )
      for {
        eventResult <- IO.fromFuture(IO(eventDao.insert(event)))
        menu = event.data.as[Menu]
        viewResult <- viewHandler.update(menu)
      } yield {
        (eventResult, viewResult) match {
          case (1, 1) =>
            viewHandler.sendMenuToAllUsers(menu).unsafeRunSync()
            Right(ResponseMessage.SUCCESS)
          case _ =>
            Left(ResponseMessage.FAILED)
        }
      }
    }
  }

  private def getLatestSelectedMenuEvents(uuid: UUID): Future[Seq[Event]] = {
    eventDao.findByTypeAndDataUuidSortedByTimestamp(Set(EventType.MENU_SELECTED), uuid)
  }

  private def authenticate[R](menu: Menu, password: String = config.getString("write.password"))
  (accessGranted: Menu => IO[Either[String, R]]): IO[Either[String, R]] = {
    menu.passwordAttempt.fold {
      val result: IO[Either[String, R]] = IO.pure(Left(ResponseMessage.UNAUTHORIZED))
      result
    } { pa =>
      if (pa != password) {
        val result: IO[Either[String, R]] = IO.pure(Left(ResponseMessage.UNAUTHORIZED))
        result
      } else {
        accessGranted(menu)
      }
    }
  }
}
