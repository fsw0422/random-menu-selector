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

@Singleton
class Aggregate @Inject()(
  config: Config,
  auth: Auth,
  eventDao: EventDao,
  userViewDao: UserViewDao
) {

  private implicit val actorSystem = ActorSystem("UserAggregate")
  private implicit val executionContext = actorSystem.dispatcher
  private implicit val actorMaterializerSettings = ActorMaterializerSettings(actorSystem)
  private implicit val actorMaterializer = ActorMaterializer(actorMaterializerSettings)

  private val writePassword = config.getString("write.password")

  def createOrUpdateUser(user: JsValue): IO[Either[String, Option[UUID]]] = {
    auth.authenticate(user, writePassword)(IO.pure(Left(ErrorResponseMessage.UNAUTHORIZED))) {
      val newUserViewOpt = user.asOpt[UserView]
      val result = for {
        newUserView <- OptionT.fromOption[IO](newUserViewOpt)
        userViews <- OptionT.liftF(userViewDao.findByEmail(newUserView.email))
        updatedUserView = userViews.headOption.fold(newUserView)(oldUserView => update(oldUserView, newUserView))
        _ <- OptionT.liftF {
          val event = Event(
            `type` = EventType.USER_PROFILE_CREATED_OR_UPDATED,
            data = Some(Json.toJson(updatedUserView)),
          )
          eventDao.insert(event)
        }
      } yield Right(updatedUserView.uuid)
      result.value.map(_.getOrElse(Left(ErrorResponseMessage.NO_SUCH_IDENTITY)))
    }
  }

  def deleteUser(userUuid: JsValue): IO[Either[String, Option[UUID]]] = {
    auth.authenticate(userUuid, writePassword)(IO.pure(Left(ErrorResponseMessage.UNAUTHORIZED))) {
      val targetUserUuidStrOpt = (userUuid \ "uuid").asOpt[String]
      val result = for {
        targetUserUuidStr <- OptionT.fromOption[IO](targetUserUuidStrOpt)
        targetUserUuid = UUID.fromString(targetUserUuidStr)
        _ <- OptionT.liftF(userViewDao.delete(targetUserUuid))
        _ <- OptionT.liftF {
          val event = Event(
            `type` = EventType.USER_PROFILE_DELETED,
            data = Some(Json.toJson(targetUserUuid)),
          )
          eventDao.insert(event)
        }
      } yield Right(targetUserUuidStrOpt.map(UUID.fromString))
      result.value.map(_.getOrElse(Left(ErrorResponseMessage.NO_SUCH_IDENTITY)))
    }
  }

  private def update(initialUserView: UserView, userView: UserView): UserView = {
    val updatedState = State[UserView, Unit] { oldUserView =>
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
