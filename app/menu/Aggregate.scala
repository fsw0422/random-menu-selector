package menu

import java.util.UUID

import cats.effect.IO
import com.typesafe.config.Config
import event.{Event, EventHandler, EventType}
import javax.inject.{Inject, Singleton}
import play.api.libs.json.{JsObject, Json}
import utils.ResponseMessage
import utils.ResponseMessage._

final case class Menu(
  uuid: Option[UUID],
  name: Option[String],
  ingredients: Option[Seq[String]],
  recipe: Option[String],
  link: Option[String],
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
  eventHandler: EventHandler,
  menuViewHandler: MenuViewHandler
) {

  def register(menuOpt: Option[Menu]): IO[Either[String, String]] = {
    menuOpt.fold(returnError[String](ResponseMessage.PARAM_ERROR)) { menu =>
      authenticate(menu) { menu =>
        menu.validateRegisterParams(returnError[String](ResponseMessage.PARAM_MISSING)) { menu =>
          val createOrUpdateMenu = menu.uuid.fold(menu.copy(uuid = Option(UUID.randomUUID())))(uuid => menu)
          val event = Event(
            `type` = Some(EventType.MENU_CREATED),
            aggregate = Some(Menu.aggregateName),
            data = Some(Json.toJson(createOrUpdateMenu).as[JsObject] - "passwordAttempt")
          )
          for {
            eventResult <- eventHandler.insert(event)
            viewResult <- menuViewHandler.createOrUpdate(createOrUpdateMenu)
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
    menuOpt.fold(returnError[String](ResponseMessage.PARAM_ERROR)) { menu =>
      authenticate(menu) { menu =>
        menu.validateEditParams(returnError[String](ResponseMessage.PARAM_MISSING)) { menu =>
          val event = Event(
            `type` = Some(EventType.MENU_UPDATED),
            aggregate = Some(Menu.aggregateName),
            data = Some(Json.toJson(menu).as[JsObject] - "passwordAttempt")
          )
          for {
            eventResult <- eventHandler.insert(event)
            viewResult <- menuViewHandler.createOrUpdate(menu)
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
    menuOpt.fold(returnError[String](ResponseMessage.PARAM_ERROR)) { menu =>
      authenticate(menu) { menu =>
        menu.uuid.fold(returnError[String](ResponseMessage.PARAM_MISSING)) { menuUuid =>
          val event = Event(
            `type` = Some(EventType.MENU_DELETED),
            aggregate = Some(Menu.aggregateName),
            data = Some(Json.obj("uuid" -> Json.toJson(menuUuid)))
          )
          for {
            eventResult <- eventHandler.insert(event)
            viewResult <- menuViewHandler.delete(menuUuid)
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
    uuidOpt.fold(returnError[String](ResponseMessage.PARAM_ERROR)) { selectedUuid =>
      val event = Event(
        `type` = Some(EventType.MENU_SELECTED),
        aggregate = Some(Menu.aggregateName),
        data = Some(Json.obj("uuid" -> Json.toJson(selectedUuid)))
      )
      for {
        eventResult <- eventHandler.insert(event)
        viewResult <- menuViewHandler.incrementSelectedCount(selectedUuid)
        _ <- menuViewHandler.sendMenuToAllUsers(selectedUuid)
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

  private def authenticate[R](menu: Menu, password: String = config.getString("write.password"))
  (accessGranted: Menu => IO[Either[String, R]]): IO[Either[String, R]] = {
    menu.passwordAttempt.fold(returnError[R](ResponseMessage.UNAUTHORIZED)) {
      case `password` =>
        accessGranted(menu)
      case _ =>
        returnError[R](ResponseMessage.UNAUTHORIZED)
    }
  }
}
