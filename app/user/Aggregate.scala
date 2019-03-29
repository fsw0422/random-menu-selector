package user

import java.util.UUID

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, ActorMaterializerSettings}
import auth.Auth
import cats.data.OptionT
import cats.implicits._
import com.typesafe.config.Config
import event.{Event, EventService, EventType}
import javax.inject.{Inject, Singleton}
import play.api.libs.json.{JsValue, Json}
import utils.ErrorResponseMessage

import scala.concurrent.Future

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

  val password = config.getString("write.password")

  def createOrUpdateUser(user: JsValue): Future[Either[String, Option[UUID]]] = {
    auth.checkPassword(user, password) { isAuth =>
      if (!isAuth) {
        Future(Left(ErrorResponseMessage.UNAUTHORIZED))
      } else {
        val targetUserViewOpt = user.asOpt[UserView]
        val result = for {
          targetUserView <- OptionT.fromOption[Future](targetUserViewOpt)
          userViews <- OptionT.liftF(userViewService.findByEmail(targetUserView.email))
        } yield {
          val updatedUserView = userViews.headOption
            .fold(targetUserView) { userView =>
              userView.copy(
                name = targetUserView.name,
                email = targetUserView.email
              )
            }

          val event = Event(
            `type` = EventType.USER_PROFILE_CREATED_OR_UPDATED,
            data = Some(Json.toJson(updatedUserView))
          )
          eventService.userEventBus offer event

          Right(updatedUserView.uuid)
        }
        result.value
          .map(_.getOrElse(Left(ErrorResponseMessage.NO_SUCH_IDENTITY)))
      }
    }
  }

  def deleteUser(user: JsValue): Future[Either[String, Option[UUID]]] = {
    auth.checkPassword(user, password) { isAuth =>
      Future {
        if (!isAuth) {
          Left(ErrorResponseMessage.UNAUTHORIZED)
        } else {
          val userUuidStrOpt = (user \ "uuid").asOpt[String]
          Either.cond(
            userUuidStrOpt.isDefined,
            userUuidStrOpt
              .map { userUuidStr =>
                val userUuid = UUID.fromString(userUuidStr)
                userViewService.delete(userUuid)

                val event = Event(
                  `type` = EventType.USER_PROFILE_DELETED,
                  data = Some(Json.toJson(userUuid)),
                )
                eventService.menuEventBus offer event

                userUuid
              },
            ErrorResponseMessage.NO_SUCH_IDENTITY
          )
        }
      }
    }
  }

  def createOrUpdateUserViewSchema(version: JsValue): Future[Either[String, Unit]] = {
    auth.checkPassword(version, password) { isAuth =>
      Future {
        if (!isAuth) {
          Left(ErrorResponseMessage.UNAUTHORIZED)
        } else {
          val event = Event(`type` = EventType.USER_SCHEMA_EVOLVED, data = Some(version))
          eventService.userEventBus offer event
          Right()
        }
      }
    }
  }
}
