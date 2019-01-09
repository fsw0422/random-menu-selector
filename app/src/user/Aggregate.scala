package src.user

import akka.actor.ActorSystem
import akka.stream.{
  ActorMaterializer,
  ActorMaterializerSettings,
  OverflowStrategy
}
import akka.stream.scaladsl.Source
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

  /*
   * Event.scala bus stream that acts as a broker between aggregate and
   * - Event.scala Store which stores the events
   * - View component that constructs / persists view models
   */
  private val eventBus = Source
    .queue[Event](5, OverflowStrategy.backpressure)
    .to(eventService.eventHandler)
    .run()

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
      queueOfferResult <- eventBus offer Event(
        `type` = EventType.USER_PROFILE_CREATED_OR_UPDATED,
        data = Some(Json.toJson(updatedMenuView)),
        timestamp = DateTime.now
      )
    } yield queueOfferResult
  }

  def createOrUpdateUserViewSchema(version: JsValue) = {
    eventBus offer Event(
      `type` = EventType.USER_SCHEMA_EVOLVED,
      data = Some(version),
      timestamp = DateTime.now
    )
  }
}
