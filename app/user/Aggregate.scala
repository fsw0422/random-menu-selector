package user

import java.util.UUID

import cats.effect.IO
import event.{Event, EventHandler, EventType}
import javax.inject.{Inject, Singleton}
import play.api.libs.json.Json
import utils.ResponseMessage
import utils.ResponseMessage._

final case class User(
  uuid: Option[UUID] = Some(UUID.randomUUID()),
  name: Option[String],
  email: Option[String]
) {

  def validateRegisterParams[A](notValid: => A)(valid: User => A): A = {
    if (this.name.isEmpty || this.email.isEmpty) {
      notValid
    } else {
      valid.apply(this)
    }
  }

  def validateEditParams[A](notValid: => A)(valid: User => A): A = {
    this.uuid.isDefined match {
      case true =>
        valid.apply(this)
      case _ =>
        notValid
    }
  }
}

object User {

  val aggregateName = "USER"

  implicit val jsonFormat = Json.format[User]
}

@Singleton
class Aggregate @Inject()(
  eventHandler: EventHandler,
  userViewHandler: UserViewHandler
) {

  def signUp(userOpt: Option[User]): IO[Either[String, String]] = {
    userOpt.fold(returnError[String](ResponseMessage.PARAM_ERROR)) { user =>
      user.validateRegisterParams(returnError[String](ResponseMessage.PARAM_MISSING)) { user =>
        val event = Event(
          `type` = Some(EventType.USER_CREATED),
          aggregate = Some(User.aggregateName),
          data = Some(Json.toJson(user)),
        )
        for {
          eventResult <- eventHandler.insert(event)
          viewResult <- userViewHandler.createOrUpdate(user)
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

  def edit(userOpt: Option[User]): IO[Either[String, String]] = {
    userOpt.fold(returnError[String](ResponseMessage.PARAM_ERROR)) { user =>
      user.validateEditParams(returnError[String](ResponseMessage.PARAM_MISSING)) { user =>
        val event = Event(
          `type` = Some(EventType.MENU_UPDATED),
          aggregate = Some(User.aggregateName),
          data = Some(Json.toJson(user))
        )
        for {
          eventResult <- eventHandler.insert(event)
          viewResult <- userViewHandler.createOrUpdate(user)
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

  def remove(userOpt: Option[User]): IO[Either[String, String]] = {
    userOpt.fold(returnError[String](ResponseMessage.PARAM_ERROR)) { user =>
      user.uuid.fold(returnError[String](ResponseMessage.PARAM_MISSING)) { userUuid =>
        val event = Event(
          `type` = Some(EventType.MENU_DELETED),
          aggregate = Some(User.aggregateName),
          data = Some(Json.toJson(userUuid))
        )
        for {
          eventResult <- eventHandler.insert(event)
          viewResult <- userViewHandler.delete(userUuid)
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
