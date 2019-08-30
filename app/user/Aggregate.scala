package user

import java.util.UUID

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, ActorMaterializerSettings}
import auth.Auth
import cats.data.{OptionT, State}
import cats.effect.IO
import com.typesafe.config.Config
import event.{Event, EventDao, EventType}
import javax.inject.{Inject, Singleton}
import play.api.libs.json.{JsObject, JsValue, Json}
import utils.ResponseMessage

final case class User(
  uuid: Option[UUID] = Some(UUID.randomUUID()),
  name: String,
  email: String
)

object User {

  implicit val jsonFormat = Json
    .using[Json.WithDefaultValues]
    .format[User]
}

@Singleton
class Aggregate @Inject()(
  config: Config,
  auth: Auth,
  eventDao: EventDao,
  userViewDao: UserViewDao
) {

  private val writePassword = config.getString("write.password")

  def createOrUpdateUser(body: JsValue): IO[Either[String, String]] = {
    auth.authenticate(body, writePassword)(IO.pure(Left(ResponseMessage.UNAUTHORIZED))) {
      val userJson = Json.toJson(body.as[JsObject] - "password")
      val result = for {
        response <- OptionT.liftF {
          val event = Event(
            `type` = EventType.USER_CREATED_OR_UPDATED,
            data = Some(userJson)
          )
          eventDao.insert(event)
        }
        user <- OptionT.fromOption[IO](userJson.asOpt[User])
        _ <- OptionT.liftF(userViewDao.upsert(user))
      } yield {
        response match {
          case 1 => Right(ResponseMessage.SUCCESS)
          case _ => Left(ResponseMessage.FAILED)
        }
      }
      result.value.map(_.getOrElse(Left(ResponseMessage.NO_SUCH_IDENTITY)))
    }
  }

  def deleteUser(body: JsValue): IO[Either[String, String]] = {
    auth.authenticate(body, writePassword)(IO.pure(Left(ResponseMessage.UNAUTHORIZED))) {
      val targetUserUuidJson = Json.toJson(body.as[JsObject] - "password")
      val result = for {
        response <- OptionT.liftF {
          val event = Event(
            `type` = EventType.USER_DELETED,
            data = Some(targetUserUuidJson)
          )
          eventDao.insert(event)
        }
        targetUserUuid <- OptionT.fromOption[IO]((targetUserUuidJson \ "uuid").asOpt[String].map(UUID.fromString))
        _ <- OptionT.liftF(userViewDao.delete(targetUserUuid))
      } yield {
        response match {
          case 1 => Right(ResponseMessage.SUCCESS)
          case _ => Left(ResponseMessage.FAILED)
        }
      }
      result.value.map(_.getOrElse(Left(ResponseMessage.NO_SUCH_IDENTITY)))
    }
  }

  private def update(initialUserView: User, userView: User): User = {
    val updatedState = State[User, Unit] { oldUserView =>
      val newUserView = oldUserView.copy(
        email = userView.email,
        name = userView.name
      )
      (newUserView, ())
    }
    val (updated, void) = updatedState.run(initialUserView).value
    updated
  }
}
