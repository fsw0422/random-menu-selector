package src.event

import java.util.UUID
import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, ActorMaterializerSettings, OverflowStrategy}
import akka.stream.ThrottleMode.Shaping
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
import scala.concurrent.duration._

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
                             userViewService: UserViewService) {
  private implicit val actorSystem = ActorSystem("Event")
  private implicit val executionContext = actorSystem.dispatcher
  private implicit val actorMaterializerSettings = ActorMaterializerSettings(
    actorSystem
  )
  private implicit val actorMaterializer = ActorMaterializer(
    actorMaterializerSettings
  )

  val storeEvent = Sink.foreach[Event] { event =>
    event.`type` match {
      case EventType.MENU_SCHEMA_EVOLVED | EventType.USER_SCHEMA_EVOLVED =>
        viewDatabase.viewVersionNonExistAction(event)(
          targetVersion => eventDao.insert(event)
        )
      case _ =>
        eventDao.insert(event)
    }
  }

  /*
   * This method is to replay and construct view
   */
  val viewEventBus = Source
    .queue[Event](5, OverflowStrategy.backpressure)
    .throttle(1, 2 seconds, 3, Shaping)
    .alsoTo(menuViewService.constructView)
    .to(userViewService.constructView)
    .run()

  /*
   * This method is to replay and construct view
   */
  def replay(from: Long) = {
    eventDao
      .findByTimeStamp(new DateTime(from))
      .map(events => events.map(event => viewEventBus offer event))
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
