package user

import java.util.UUID

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, ActorMaterializerSettings, QueueOfferResult}
import auth.Auth
import cats.data.{OptionT, State}
import cats.effect.IO
import com.typesafe.config.Config
import event.{Event, EventService, EventType}
import javax.inject.{Inject, Singleton}
import play.api.libs.json.{JsValue, Json}
import utils.ErrorResponseMessage

@Singleton
class Aggregate @Inject()(
  config: Config,
  auth: Auth,
  eventService: EventService,
  userViewService: UserViewService
) {

  private implicit val actorSystem = ActorSystem("UserAggregate")
  private implicit val executionContext = actorSystem.dispatcher
  private implicit val actorMaterializerSettings = ActorMaterializerSettings(actorSystem)
  private implicit val actorMaterializer = ActorMaterializer(actorMaterializerSettings)

  private val password = config.getString("write.password")

  def createOrUpdateUser(user: JsValue): IO[Either[String, Option[UUID]]] = {
    auth.checkPassword(user, password) { isAuth =>
      if (!isAuth) {
        IO.pure(Left(ErrorResponseMessage.UNAUTHORIZED))
      } else {
        val newUserViewOpt = user.asOpt[UserView]
        val result = for {
          newUserView <- OptionT.fromOption[IO](newUserViewOpt)
          userViews <- OptionT.liftF(userViewService.findByEmail(newUserView.email))
          updatedUserView = userViews.headOption.fold(newUserView)(oldUserView => update(oldUserView, newUserView))
          _ <- OptionT.liftF {
            val event = Event(
              `type` = EventType.USER_PROFILE_CREATED_OR_UPDATED,
              data = Some(Json.toJson(updatedUserView)),
            )
            IO.fromFuture(IO(eventService.userEventBus offer event))
          }
        } yield Right(updatedUserView.uuid)
        result.value.map(_.getOrElse(Left(ErrorResponseMessage.NO_SUCH_IDENTITY)))
      }
    }
  }

  def deleteUser(user: JsValue): IO[Either[String, Option[UUID]]] = {
    auth.checkPassword(user, password) { isAuth =>
      if (!isAuth) {
        IO.pure(Left(ErrorResponseMessage.UNAUTHORIZED))
      } else {
        val targetUserUuidStrOpt = (user \ "uuid").asOpt[String]
        val result = for {
          targetUserUuidStr <- OptionT.fromOption[IO](targetUserUuidStrOpt)
          targetUserUuid = UUID.fromString(targetUserUuidStr)
          _ <- OptionT.liftF(userViewService.delete(targetUserUuid))
          _ <- OptionT.liftF {
            val event = Event(
              `type` = EventType.USER_PROFILE_DELETED,
              data = Some(Json.toJson(targetUserUuid)),
            )
            IO.fromFuture(IO(eventService.menuEventBus offer event))
          }
        } yield Right(targetUserUuidStrOpt.map(UUID.fromString))
        result.value.map(_.getOrElse(Left(ErrorResponseMessage.NO_SUCH_IDENTITY)))
      }
    }
  }

  def createOrUpdateUserViewSchema(version: JsValue): IO[Either[String, QueueOfferResult]] = {
    auth.checkPassword(version, password) { isAuth =>
      if (!isAuth) {
        IO.pure(Left(ErrorResponseMessage.UNAUTHORIZED))
      } else {
        val event = Event(`type` = EventType.USER_SCHEMA_EVOLVED, data = Some(version))
        IO.fromFuture(IO((eventService.userEventBus offer event).map(Right(_))))
      }
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
