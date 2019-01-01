package src.user

import akka.actor.ActorSystem
import akka.stream.scaladsl.Sink
import com.typesafe.scalalogging.LazyLogging
import play.api.libs.json.Json
import src.EventType.EventType
import src.{Event, EventDao, EventType}
import scala.concurrent.Future

case class UserView(id: Option[Long] = None,
                    name: String = "NONE",
                    email: String = "NONE")

object UserView {
  import src.utils.mapper.JsMapper._

  implicit val jsonFormat =
    Json.using[Json.WithDefaultValues].format[UserView]
}

object UserViewService {

  val constructView = Sink.foreach[Event] { event =>
    event.`type` match {
      case EventType.USER_PROFILE_CREATED_OR_UPDATED =>
        val userView = event.data.as[UserView]
        UserViewDao.upsert(userView)
      case EventType.USER_SCHEMA_CREATED_OR_UPDATED =>
        val targetVersion = (event.data \ "version").as[String]
        UserViewDao.evolve(event.`type`, targetVersion)
    }
  }

  def findAll() = {
    UserViewDao.findAll()
  }
}

object UserViewDao extends LazyLogging {

  import slick.jdbc.H2Profile.api._

  private implicit val actorSystem = ActorSystem("UserViewDao")
  private implicit val executionContext = actorSystem.dispatcher

  class UserViewTable(tag: Tag) extends Table[UserView](tag, "USER_VIEW") {
    def id = column[Long]("ID", O.PrimaryKey, O.AutoInc)
    def name = column[String]("NAME")
    def email = column[String]("EMAIL")

    def * =
      (id.?, name, email) <> ((UserView.apply _).tupled, UserView.unapply)
  }

  private val userViewTable = TableQuery[UserViewTable]

  private val db = Database.forConfig("h2")

  def upsert(userView: UserView) = {
    db.run(userViewTable.insertOrUpdate(userView))
  }

  def findAll(): Future[Seq[UserView]] = {
    db.run(userViewTable.result)
  }

  def evolve(eventType: EventType, targetVersion: String) = {
    EventDao.findByType(eventType).map { events =>
      val sameVersionExists = events
        .exists(event => (event.data \ "version").as[String] == targetVersion)
      if (!sameVersionExists) {
        targetVersion match {
          case "1.0" =>
            db.run(userViewTable.schema.create)
          case _ =>
            logger.warn(s"No version change is defined with $targetVersion")
        }
      } else {
        logger.warn(s"Event with $targetVersion already exists")
      }
    }
  }
}
