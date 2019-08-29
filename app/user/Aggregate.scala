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
import play.api.libs.json.{JsValue, Json}
import utils.ErrorResponseMessage

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

  def createOrUpdateUser(user: JsValue): IO[Either[String, Int]] = {
    auth.authenticate(user, writePassword)(IO.pure(Left(ErrorResponseMessage.UNAUTHORIZED))) {
      val result = for {
        res <- OptionT.liftF {
          val event = Event(
            `type` = EventType.USER_PROFILE_CREATED_OR_UPDATED,
            data = Some(user),
          )
          eventDao.insert(event)
        }
        // TODO: from here should be updated by an event as separate module
        newUser <- OptionT.fromOption[IO](user.asOpt[User])
        users <- OptionT.liftF(userViewDao.findByEmail(newUser.email))
        updatedUser = users.headOption.fold(newUser)(oldUser => update(oldUser, newUser))
        _ <- OptionT.liftF(userViewDao.upsert(updatedUser))
      } yield Right(res)
      result.value.map(_.getOrElse(Left(ErrorResponseMessage.NO_SUCH_IDENTITY)))
    }
  }

  def deleteUser(userUuid: JsValue): IO[Either[String, Int]] = {
    auth.authenticate(userUuid, writePassword)(IO.pure(Left(ErrorResponseMessage.UNAUTHORIZED))) {
      val result = for {
        res <- OptionT.liftF {
          val event = Event(
            `type` = EventType.USER_PROFILE_DELETED,
            data = Some(userUuid),
          )
          eventDao.insert(event)
        }
        // TODO: from here should be updated by an event as separate module
        targetUserUuid <- OptionT.fromOption[IO]((userUuid \ "uuid").asOpt[String].map(UUID.fromString))
        _ <- OptionT.liftF(userViewDao.delete(targetUserUuid))
      } yield Right(res)
      result.value.map(_.getOrElse(Left(ErrorResponseMessage.NO_SUCH_IDENTITY)))
    }
  }

  //TODO: try to find a way to compose rather than running
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
