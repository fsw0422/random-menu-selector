package src.user

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, ActorMaterializerSettings}
import javax.inject.{Inject, Singleton}
import monocle.macros.GenLens
import org.joda.time.DateTime
import play.api.libs.json.{JsValue, Json}
import src.event.{Event, EventService, EventType}

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
      updatedMenuView <- userViewService
        .findByEmail(userView.email)
        .map { userViews =>
          val lens = GenLens[UserView]
          val targetUserView = if (userViews.nonEmpty) {
            val nameMod = lens(_.name)
              .modify(name => userView.name)(userViews.head)
            lens(_.email).modify(email => nameMod.email)(nameMod)
          } else {
            userView
          }
          targetUserView
        }
      queueOfferResult <- eventService.userEventBus offer Event(
        `type` = EventType.USER_PROFILE_CREATED_OR_UPDATED,
        data = Some(Json.toJson(updatedMenuView)),
        timestamp = DateTime.now
      )
    } yield queueOfferResult
  }

  def createOrUpdateUserViewSchema(version: JsValue) = {
    eventService.userEventBus offer Event(
      `type` = EventType.USER_SCHEMA_EVOLVED,
      data = Some(version),
      timestamp = DateTime.now
    )
  }
}
