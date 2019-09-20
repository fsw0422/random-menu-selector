package event

import java.util.UUID

import event.EventType.EventType
import javax.inject.Singleton
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
  uuid: UUID,
  timestamp: Option[DateTime],
  `type`: Option[EventType],
  aggregate: Option[String],
  data: Option[JsValue]
) {

  def validate[A](notValid: => A)(valid: Event => A): A = _ //TODO: implement
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

  class EventTable(tag: Tag) extends Table[Event](tag, "event") {
    def uuid = column[UUID]("uuid", O.PrimaryKey)
    def timestamp = column[DateTime]("timestamp")
    def `type` = column[EventType]("type")
    def version = column[String]("aggregate")
    def data = column[JsValue]("data")

    def * =
      (uuid, timestamp.?, `type`.?, version.?, data.?) <> ((Event.apply _).tupled, Event.unapply)
  }

  private lazy val eventTable = TableQuery[EventTable]

  private lazy val db = DatabaseConfig.forConfig[JdbcProfile]("postgres").db

  def insert(event: Event): Future[Int] = db.run {
    eventTable += event
  }

  def findByTypeAndDataUuidSortedByTimestamp(`types`: Set[EventType], uuid: UUID): Future[Seq[Event]] = db.run {
    eventTable
      .filter(event => event.`type` inSet `types`)
      .filter(event => event.data +>> "uuid" === uuid.toString)
      .sortBy(event => event.timestamp.desc)
      .result
  }
}
