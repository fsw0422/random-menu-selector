package menu

import java.util.UUID

import cats.effect.IO
import com.typesafe.config.Config
import event.{Event, EventDao, EventType}
import javax.inject.{Inject, Singleton}
import play.api.libs.json.Json
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
    this.uuid.isDefined match {
      case true =>
        valid.apply(this)
      case _ =>
        notValid
    }
  }
}

object Menu {

  val aggregateName = "MENU"

  implicit val jsonFormatter = Json.format[Menu]
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
            uuid = genericToolset.randomUUID(),
            `type` = Some(EventType.MENU_CREATED),
            aggregate = Some(Menu.aggregateName),
            data = Some(Json.toJson(newMenu)),
            timestamp = Some(genericToolset.currentTime())
          )
          for {
            eventResult <- IO.fromFuture(IO(eventDao.insert(event)))
            viewResult <- viewHandler.createOrUpdate(newMenu)
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
      authenticate(menu) { menu =>
        menu.validateEditParams {
          val result: IO[Either[String, String]] = IO.pure(Left(ResponseMessage.PARAM_MISSING))
          result
        } { menu =>
          val event = Event(
            uuid = genericToolset.randomUUID(),
            `type` = Some(EventType.MENU_UPDATED),
            aggregate = Some(Menu.aggregateName),
            data = Some(Json.toJson(menu)),
            timestamp = Some(genericToolset.currentTime())
          )
          for {
            eventResult <- IO.fromFuture(IO(eventDao.insert(event)))
            viewResult <- viewHandler.createOrUpdate(menu)
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
        menu.uuid.fold {
          val result: IO[Either[String, String]] = IO.pure(Left(ResponseMessage.PARAM_MISSING))
          result
        } { menuUuid =>
          val event = Event(
            uuid = genericToolset.randomUUID(),
            `type` = Some(EventType.MENU_DELETED),
            aggregate = Some(Menu.aggregateName),
            data = Some(Json.obj("uuid" -> Json.toJson(menuUuid))),
            timestamp = Some(genericToolset.currentTime())
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
  }

  def selectMenu(uuidOpt: Option[UUID]): IO[Either[String, String]] = {
    uuidOpt.fold {
      val result: IO[Either[String, String]] = IO.pure(Left(ResponseMessage.PARAM_ERROR))
      result
    } { selectedUuid =>
      for {
        latestSelectedMenuEvents <- IO.fromFuture(IO(getLatestSelectedMenuEvents(selectedUuid)))
        emptyMenu = Menu(
          uuid = Some(selectedUuid),
          name = None,
          ingredients = None,
          recipe = None,
          link = None,
          selectedCount = Some(0),
          passwordAttempt = None
        )
        newMenu = latestSelectedMenuEvents.headOption.fold(emptyMenu) { latestSelectedMenuEvent =>
          latestSelectedMenuEvent.data.fold(emptyMenu) { data =>
            val latestSelectedMenu = data.as[Menu]
            latestSelectedMenu.copy(selectedCount = latestSelectedMenu.selectedCount.map(_ + 1))
          }
        }
        event = Event(
          uuid = genericToolset.randomUUID(),
          `type` = Some(EventType.MENU_SELECTED),
          aggregate = Some(Menu.aggregateName),
          data = Some(Json.toJson(newMenu)),
          timestamp = Some(genericToolset.currentTime())
        )
        eventResult <- IO.fromFuture(IO(eventDao.insert(event)))
        viewResult <- viewHandler.createOrUpdate(newMenu)
        _ <- viewHandler.sendMenuToAllUsers(newMenu)
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
