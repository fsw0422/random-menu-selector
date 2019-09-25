package user

import java.util.UUID

import cats.effect.IO
import event.{Event, EventHandler, EventType}
import javax.inject.{Inject, Singleton}
import play.api.libs.json.Json
import utils.{GenericToolset, ResponseMessage}

final case class User(
  uuid: Option[UUID],
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
  genericToolset: GenericToolset,
  eventHandler: EventHandler,
  userViewHandler: UserViewHandler
) {

  def signUp(userOpt: Option[User]): IO[Either[String, String]] = {
    userOpt.fold {
      val result: IO[Either[String, String]] = IO.pure(Left(ResponseMessage.PARAM_ERROR))
      result
    } { user =>
      user.validateRegisterParams {
        val result: IO[Either[String, String]] = IO.pure(Left(ResponseMessage.PARAM_MISSING))
        result
      } { user =>
        val newUser = user.copy(uuid = Some(genericToolset.randomUUID()))
        val event = Event(
          uuid = genericToolset.randomUUID(),
          `type` = Some(EventType.USER_CREATED),
          aggregate = Some(User.aggregateName),
          data = Some(Json.toJson(newUser)),
          timestamp = Some(genericToolset.currentTime())
        )
        for {
          eventResult <- eventHandler.insert(event)
          viewResult <- userViewHandler.createOrUpdate(newUser)
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
    userOpt.fold {
      val result: IO[Either[String, String]] = IO.pure(Left(ResponseMessage.PARAM_ERROR))
      result
    } { user =>
      user.validateEditParams {
        val result: IO[Either[String, String]] = IO.pure(Left(ResponseMessage.PARAM_MISSING))
        result
      } { user =>
        val event = Event(
          uuid = genericToolset.randomUUID(),
          `type` = Some(EventType.MENU_UPDATED),
          aggregate = Some(User.aggregateName),
          data = Some(Json.toJson(user)),
          timestamp = Some(genericToolset.currentTime())
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
    userOpt.fold {
      val result: IO[Either[String, String]] = IO.pure(Left(ResponseMessage.PARAM_ERROR))
      result
    } { user =>
      user.uuid.fold {
        val result: IO[Either[String, String]] = IO.pure(Left(ResponseMessage.PARAM_MISSING))
        result
      } { userUuid =>
        val event = Event(
          uuid = genericToolset.randomUUID(),
          `type` = Some(EventType.MENU_DELETED),
          aggregate = Some(User.aggregateName),
          data = Some(Json.toJson(userUuid)),
          timestamp = Some(genericToolset.currentTime())
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
