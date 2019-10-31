package event

import java.util.UUID

import cats.effect.IO
import event.EventType.EventType
import javax.inject.{Inject, Singleton}
import org.joda.time.DateTime
import play.api.libs.json.JsValue
import slick.basic.DatabaseConfig
import slick.jdbc.JdbcProfile

import scala.concurrent.Future

object EventType extends Enumeration {
  type EventType = Value

  val
  MENU_CREATED, MENU_UPDATED, MENU_DELETED, MENU_SELECTED,
  USER_CREATED, USER_UPDATED, USER_DELETED
  = Value
}

final case class Event(
  uuid: Option[UUID] = Some(UUID.randomUUID()),
  timestamp: Option[DateTime] = Some(DateTime.now()),
  `type`: Option[EventType],
  aggregate: Option[String],
  data: Option[JsValue]
)

@Singleton
class EventHandler @Inject()(eventDao: EventDao) {

  def insert(event: Event): IO[Int] = {
    IO.fromFuture(IO(eventDao.insert(event)))
  }

  def findByTypeAndDataUuidSortedByTimestamp(`types`: Set[EventType], uuid: UUID): IO[Seq[Event]] = {
    IO.fromFuture(IO(eventDao.findByTypeAndDataUuidSortedByTimestamp(`types`, uuid)))
  }
}

@Singleton
class EventDao {

  import utils.db.PostgresProfile.MappedColumnType
  import utils.db.PostgresProfile.api._

  implicit val eventTypeMapper =
    MappedColumnType.base[EventType.Value, String](
      enum => enum.toString,
      str => EventType.withName(str)
    )

  /*
   * Table definition is defined in the Database itself (see db/changelog.xml)
   * It is not the same thing as the object definition
   */
  class EventTable(tag: Tag) extends Table[Event](tag, "event") {
    def uuid = column[UUID]("uuid")
    def timestamp = column[DateTime]("timestamp")
    def `type` = column[EventType]("type")
    def aggregate = column[String]("aggregate")
    def data = column[JsValue]("data")

    def * =
      (uuid.?, timestamp.?, `type`.?, aggregate.?, data.?) <> ((Event.apply _).tupled, Event.unapply)
  }

  private lazy val eventTable = TableQuery[EventTable]

  private lazy val db = DatabaseConfig.forConfig[JdbcProfile]("postgres").db

  def insert(event: Event): Future[Int] = db.run {
    eventTable.map(t => (t.`type`.?, t.aggregate.?, t.data.?)) += ((event.`type`, event.aggregate, event.data))
  }

  def findByTypeAndDataUuidSortedByTimestamp(`types`: Set[EventType], uuid: UUID): Future[Seq[Event]] = db.run {
    eventTable
      .filter(event => event.`type` inSet `types`)
      .filter(event => event.data +>> "uuid" === uuid.toString)
      .sortBy(event => event.timestamp.desc)
      .result
  }
}
