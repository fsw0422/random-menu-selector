package event

import java.util.UUID

import akka.actor.ActorSystem
import akka.stream.scaladsl.{Flow, Sink, Source}
import akka.stream.{ActorMaterializer, ActorMaterializerSettings, OverflowStrategy}
import com.typesafe.scalalogging.LazyLogging
import event.EventType.EventType
import javax.inject.{Inject, Singleton}
import menu.{MenuView, MenuViewService}
import org.joda.time.DateTime
import play.api.libs.json.JsValue
import user.{UserView, UserViewService}
import utils.db.{Dao, ViewDatabase}

import scala.concurrent.Future

object EventType extends Enumeration {
  type EventType = Value
  val UNKNOWN, RANDOM_MENU_ASKED, MENU_PROFILE_CREATED_OR_UPDATED,
  MENU_PROFILE_DELETED, MENU_SCHEMA_EVOLVED, USER_PROFILE_CREATED_OR_UPDATED,
  USER_PROFILE_DELETED, USER_SCHEMA_EVOLVED =
    Value
}

case class Event(uuid: Option[UUID] = Some(UUID.randomUUID()),
                 timestamp: DateTime = DateTime.now(),
                 `type`: EventType = EventType.UNKNOWN,
                 data: Option[JsValue] = None)

@Singleton
class EventService @Inject()(eventDao: EventDao,
                             viewDatabase: ViewDatabase,
                             menuViewService: MenuViewService,
                             userViewService: UserViewService)
    extends LazyLogging {

  private implicit val actorSystem = ActorSystem("Event")
  private implicit val executionContext = actorSystem.dispatcher
  private implicit val actorMaterializerSettings = ActorMaterializerSettings(
    actorSystem
  )
  private implicit val actorMaterializer = ActorMaterializer(
    actorMaterializerSettings
  )

  val menuEventBus = eventStream(5) { event =>
    event.`type` match {
      case EventType.RANDOM_MENU_ASKED |
          EventType.MENU_PROFILE_CREATED_OR_UPDATED |
          EventType.MENU_PROFILE_DELETED | EventType.MENU_SCHEMA_EVOLVED =>
        eventDao.insert(event)
      case _ =>
        logger.error(s"No such event type [${event.`type`}]")
    }
    event
  } { event =>
    event.`type` match {
      case EventType.RANDOM_MENU_ASKED |
          EventType.MENU_PROFILE_CREATED_OR_UPDATED =>
        event.data
        .fold(logger.warn(s"[$event] is null")) { menuViewJson =>
          menuViewService.upsert(menuViewJson.as[MenuView])
        }
      case EventType.MENU_PROFILE_DELETED =>
        event.data
        .fold(logger.warn(s"[$event] is null")) { menuUuidJson =>
          menuViewService.delete(menuUuidJson.as[UUID])
        }
      case EventType.MENU_SCHEMA_EVOLVED =>
        viewDatabase.viewVersionNonExistAction(event)(
          targetVersion => menuViewService.evolve(targetVersion)
        )
      case _ =>
        logger.error(s"No such event type [${event.`type`}]")
    }
  }

  val userEventBus = eventStream(5) { event =>
    event.`type` match {
      case EventType.USER_PROFILE_CREATED_OR_UPDATED |
          EventType.USER_PROFILE_DELETED | EventType.USER_SCHEMA_EVOLVED =>
        eventDao.insert(event)
      case _ =>
        logger.error(s"No such event type [${event.`type`}]")
    }
    event
  } { event =>
    event.`type` match {
      case EventType.USER_PROFILE_CREATED_OR_UPDATED =>
        event.data
        .fold(logger.warn(s"[$event] is null")) { menuViewJson =>
          userViewService.upsert(menuViewJson.as[UserView])
        }
      case EventType.USER_PROFILE_DELETED =>
        event.data
        .fold(logger.warn(s"[$event] is null")) { menuUuidJson =>
          menuViewService.delete(menuUuidJson.as[UUID])
        }
      case EventType.USER_SCHEMA_EVOLVED =>
        viewDatabase.viewVersionNonExistAction(event)(
          targetVersion => userViewService.evolve(targetVersion)
        )
      case _ =>
        logger.error(s"No such event type [${event.`type`}]")
    }
  }

  private def eventStream(
    bufferSize: Int
  )(flowHandler: Event => Event)(sinkHandler: Event => Any) = {
    Source
      .queue[Event](bufferSize, OverflowStrategy.backpressure)
      .via(Flow[Event].map(event => flowHandler.apply(event)))
      .to(Sink.foreach[Event](event => sinkHandler.apply(event)))
      .run()
  }
}

@Singleton
class EventDao extends Dao with LazyLogging {

  import utils.db.PostgresProfile.api._

  implicit val eventTypeMapper =
    MappedColumnType.base[EventType.Value, String](
      enum => enum.toString,
      str => EventType.withName(str)
    )

  class EventTable(tag: Tag) extends Table[Event](tag, "event") {
    def uuid = column[UUID]("uuid", O.PrimaryKey, O.Default(UUID.randomUUID()))
    def timestamp = column[DateTime]("timestamp", O.Default(DateTime.now()))
    def `type` = column[EventType]("type", O.Default(EventType.UNKNOWN))
    def data = column[JsValue]("data")

    def * =
      (uuid.?, timestamp, `type`, data.?) <> (Event.tupled, Event.unapply)
  }

  private val eventTable = TableQuery[EventTable]

  def insert(event: Event) = {
    db.run(eventTable += event)
  }

  def findByTimeStamp(startTime: DateTime): Future[Seq[Event]] = {
    db.run(
      eventTable
        .filter(event => event.timestamp >= startTime)
        .result
    )
  }

  def findByType(`type`: EventType): Future[Seq[Event]] = {
    db.run(
      eventTable
        .filter(event => event.`type` === `type`)
        .result
    )
  }

  // Initial creation of Event table
  // Define event database evolution here
  db.run(eventTable.schema.create)
}
