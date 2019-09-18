package user

import java.util.UUID

import cats.effect.IO
import event.{Event, EventDao, EventType}
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

  implicit val jsonFormat = Json
    .using[Json.WithDefaultValues]
    .format[User]
}

@Singleton
class Aggregate @Inject()(
  genericToolset: GenericToolset,
  eventDao: EventDao,
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
          uuid = Some(genericToolset.randomUUID()),
          `type` = EventType.USER_CREATED,
          aggregate = User.aggregateName,
          data = Json.toJson(newUser),
          timestamp = genericToolset.currentTime()
        )
        for {
          eventResult <- IO.fromFuture(IO(eventDao.insert(event)))
          viewResult <- userViewHandler.create(newUser)
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
          uuid = Some(genericToolset.randomUUID()),
          `type` = EventType.MENU_UPDATED,
          aggregate = User.aggregateName,
          data = Json.toJson(user),
          timestamp = genericToolset.currentTime()
        )
        for {
          eventResult <- IO.fromFuture(IO(eventDao.insert(event)))
          viewResult <- userViewHandler.update(user)
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

  def remove(uuidOpt: Option[UUID]): IO[Either[String, String]] = {
    uuidOpt.fold {
      val result: IO[Either[String, String]] = IO.pure(Left(ResponseMessage.PARAM_ERROR))
      result
    } { userUuid =>
      val event = Event(
        uuid = Some(genericToolset.randomUUID()),
        `type` = EventType.MENU_DELETED,
        aggregate = User.aggregateName,
        data = Json.toJson(userUuid),
        timestamp = genericToolset.currentTime()
      )
      for {
        eventResult <- IO.fromFuture(IO(eventDao.insert(event)))
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
