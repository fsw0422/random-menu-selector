package src.user

import akka.actor.ActorSystem
import akka.stream.{
  ActorMaterializer,
  ActorMaterializerSettings,
  OverflowStrategy
}
import akka.stream.ThrottleMode.Shaping
import akka.stream.scaladsl.Source
import com.typesafe.scalalogging.LazyLogging
import javax.inject.{Inject, Singleton}
import play.api.libs.json.JsValue
import src.{Event, EventService, EventType}
import scala.concurrent.duration._

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
    .throttle(1, 2 seconds, 3, Shaping)
    .alsoTo(eventService.storeEvent)
    .to(userViewService.constructView)
    .run()

  def createOrUpdateUser(user: JsValue) = {
    eventBus offer Event(
      `type` = EventType.USER_PROFILE_CREATED_OR_UPDATED,
      data = user
    )
  }

  def createOrUpdateUserViewSchema(version: JsValue) = {
    eventBus offer Event(`type` = EventType.USER_SCHEMA_EVOLVED, data = version)
  }
}
