package event

import java.util.UUID

import cats.effect.IO
import com.typesafe.scalalogging.LazyLogging
import event.EventType.EventType
import javax.inject.Singleton
import org.joda.time.DateTime
import play.api.libs.json.JsValue
import utils.db.Db

object EventType extends Enumeration {
  type EventType = Value
  val UNKNOWN, RANDOM_MENU_ASKED, MENU_PROFILE_CREATED_OR_UPDATED,
  MENU_PROFILE_DELETED, MENU_SCHEMA_EVOLVED, USER_PROFILE_CREATED_OR_UPDATED,
  USER_PROFILE_DELETED, USER_SCHEMA_EVOLVED = Value
}

final case class Event(
  uuid: Option[UUID] = Some(UUID.randomUUID()),
  timestamp: DateTime = DateTime.now(),
  `type`: EventType = EventType.UNKNOWN,
  data: Option[JsValue] = None
)

@Singleton
class EventDao extends Db with LazyLogging {

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

  override def setup(): IO[Unit] = IO.fromFuture {
    IO { db.run(eventTable.schema.create) }
  }

  override def teardown(): IO[Unit] = IO.fromFuture {
    IO { db.run(eventTable.schema.drop) }
  }

  def insert(event: Event): IO[Int] = IO.fromFuture {
    IO { db.run(eventTable += event)}
  }

  def findByTimeStamp(startTime: DateTime): IO[Seq[Event]] = IO.fromFuture {
    IO {
      db.run {
        eventTable
          .filter(event => event.timestamp >= startTime)
          .result
      }
    }
  }

  def findByType(`type`: EventType): IO[Seq[Event]] = IO.fromFuture {
    IO {
      db.run {
        eventTable
          .filter(event => event.`type` === `type`)
          .result
      }
    }
  }
}
