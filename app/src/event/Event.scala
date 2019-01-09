package src.event

import java.util.UUID
import akka.actor.ActorSystem
import akka.stream.{
  ActorMaterializer,
  ActorMaterializerSettings,
  OverflowStrategy
}
import akka.stream.scaladsl.{Sink, Source}
import com.typesafe.scalalogging.LazyLogging
import javax.inject.{Inject, Singleton}
import org.joda.time.DateTime
import play.api.libs.json.JsValue
import src.event.EventType.EventType
import src.menu.MenuViewService
import src.user.UserViewService
import src.utils.db.{Dao, ViewDatabase}

import scala.concurrent.Future

object EventType extends Enumeration {
  type EventType = Value
  val UNKNOWN, RANDOM_MENU_ASKED, MENU_PROFILE_CREATED_OR_UPDATED,
  MENU_SCHEMA_EVOLVED, USER_PROFILE_CREATED_OR_UPDATED, USER_SCHEMA_EVOLVED =
    Value
}

case class Event(uuid: Option[UUID] = Some(UUID.randomUUID()),
                 timestamp: DateTime,
                 `type`: EventType,
                 data: Option[JsValue])

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

  val menuViewEventBus = Source
    .queue[Event](5, OverflowStrategy.backpressure)
    .to(menuViewService.constructView)
    .run()

  val userViewEventBus = Source
    .queue[Event](5, OverflowStrategy.backpressure)
    .to(userViewService.constructView)
    .run()

  val eventHandler = Sink.foreach[Event] { event =>
    event.`type` match {
      case EventType.RANDOM_MENU_ASKED |
          EventType.MENU_PROFILE_CREATED_OR_UPDATED |
          EventType.MENU_SCHEMA_EVOLVED =>
        menuViewEventBus offer event
      case EventType.USER_PROFILE_CREATED_OR_UPDATED |
          EventType.USER_SCHEMA_EVOLVED =>
        userViewEventBus offer event
    }
    eventDao.insert(event)
  }
}

@Singleton
class EventDao extends Dao with LazyLogging {

  import src.utils.db.PostgresProfile.api._

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
