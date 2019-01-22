package user

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, ActorMaterializerSettings}
import javax.inject.{Inject, Singleton}
import monocle.macros.GenLens
import play.api.libs.json.{JsValue, Json}
import event.{Event, EventService, EventType}
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

  def createOrUpdateUser(user: JsValue) = {
    val userView = user.as[UserView]
    for {
      updatedUserView <- userViewService
        .findByEmail(userView.email)
        .map { userViews =>
          if (userViews.nonEmpty) {
            userViews.head.copy(name = userView.name, email = userView.email)
          } else {
            userView
          }
        }
    } yield {
      val event = Event(
        `type` = EventType.USER_PROFILE_CREATED_OR_UPDATED,
        data = Some(Json.toJson(updatedUserView))
      )
      eventService.userEventBus offer event

      updatedUserView.uuid.get
    }
  }

  def createOrUpdateUserViewSchema(version: JsValue) = {
    val event =
      Event(`type` = EventType.USER_SCHEMA_EVOLVED, data = Some(version))
    eventService.userEventBus offer event
    Future(ResponseMessage.DATABASE_EVOLUTION)
  }
}
