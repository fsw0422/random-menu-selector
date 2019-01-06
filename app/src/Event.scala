package src

import akka.actor.ActorSystem
import akka.http.scaladsl.model.DateTime
import akka.stream.{
  ActorMaterializer,
  ActorMaterializerSettings,
  OverflowStrategy
}
import akka.stream.ThrottleMode.Shaping
import akka.stream.scaladsl.{Sink, Source}
import com.typesafe.scalalogging.LazyLogging
import javax.inject.{Inject, Singleton}
import play.api.libs.json.{JsValue, Json}
import src.EventType.EventType
import src.menu.MenuViewService
import src.user.UserViewService
import scala.concurrent.Future
import scala.concurrent.duration._

object EventType extends Enumeration {
  type EventType = Value
  val UNKNOWN, RANDOM_MENU_ASKED, MENU_PROFILE_CREATED_OR_UPDATED,
  MENU_SCHEMA_EVOLVED, USER_PROFILE_CREATED_OR_UPDATED, USER_SCHEMA_EVOLVED =
    Value
}

case class Event(id: Option[Long] = None,
                 timestamp: Option[DateTime] = Some(DateTime.now),
                 `type`: EventType = EventType.UNKNOWN,
                 data: JsValue = Json.parse("{}"))

@Singleton
class EventService @Inject()(eventDao: EventDao,
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
    eventDao.insert(event)
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
      .findByTimeStamp(DateTime(from))
      .map(events => events.map(event => viewEventBus offer event))
  }
}

@Singleton
class EventDao extends LazyLogging {

  import slick.jdbc.H2Profile.api._
  import src.utils.mapper.ObjectRelationalMapper._

  implicit val eventTypeMapper =
    MappedColumnType.base[EventType.Value, String](
      enum => enum.toString,
      str => EventType.withName(str)
    )

  class EventTable(tag: Tag) extends Table[Event](tag, "EVENT") {
    def id = column[Long]("ID", O.PrimaryKey, O.AutoInc)
    def timeStamp = column[DateTime]("TIME_STAMP")
    def `type` = column[EventType]("TYPE")
    def data = column[JsValue]("DATA")

    def * =
      (id.?, timeStamp.?, `type`, data) <> (Event.tupled, Event.unapply)
  }

  private val eventTable = TableQuery[EventTable]

  private val db = Database.forConfig("h2")

  def insert(event: Event) = {
    db.run(eventTable += event)
  }

  def findByTimeStamp(startTime: DateTime): Future[Seq[Event]] = {
    db.run(
      eventTable
        .filter(event => event.timeStamp >= startTime)
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
