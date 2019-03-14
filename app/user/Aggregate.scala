package user

import java.util.UUID

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, ActorMaterializerSettings}
import event.{Event, EventService, EventType}
import javax.inject.{Inject, Singleton}
import play.api.libs.json.{JsValue, Json}
import utils.ResponseMessage

import scala.concurrent.Future

@Singleton
class Aggregate @Inject()(eventService: EventService,
                          userViewService: UserViewService) {

  private implicit val actorSystem = ActorSystem("UserAggregate")
  private implicit val executionContext = actorSystem.dispatcher
  private implicit val actorMaterializerSettings = ActorMaterializerSettings(
    actorSystem
  )
  private implicit val actorMaterializer = ActorMaterializer(
    actorMaterializerSettings
  )

  def createOrUpdateUser(user: JsValue) : Future[Option[UUID]] = {
    val userView = user.as[UserView]
    for {
      updatedUserView <- userViewService.findByEmail(userView.email)
        .map { userViews =>
          userViews.headOption
            .fold(userView) { head =>
              head.copy(name = userView.name, email = userView.email)
            }
        }
    } yield {
      val event = Event(
        `type` = EventType.USER_PROFILE_CREATED_OR_UPDATED,
        data = Some(Json.toJson(updatedUserView))
      )
      eventService.userEventBus offer event

      updatedUserView.uuid
    }
  }

  def deleteUser(user: JsValue): String = {
    val userUuidStringOption = (user \ "uuid").asOpt[String]
    userUuidStringOption
      .fold(ResponseMessage.NO_SUCH_IDENTITY) { userUuidString =>
        val userUuid = UUID.fromString(userUuidString)
        userViewService.delete(userUuid)

        val event = Event(
          `type` = EventType.USER_PROFILE_DELETED,
          data = Some(Json.toJson(userUuid)),
        )
        eventService.userEventBus offer event

        userUuidString
      }
  }

  def createOrUpdateUserViewSchema(version: JsValue): String = {
    val event = Event(`type` = EventType.USER_SCHEMA_EVOLVED, data = Some(version))
    eventService.userEventBus offer event
    ResponseMessage.DATABASE_EVOLUTION
  }
}
