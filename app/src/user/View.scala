package src.user

import akka.stream.scaladsl.Sink
import com.typesafe.scalalogging.LazyLogging
import javax.inject.{Inject, Singleton}
import play.api.libs.json.Json
import src.{Event, EventType}

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

case class UserView(id: Option[Long] = None,
                    name: String = "NONE",
                    email: String = "NONE")

object UserView {
  import src.utils.mapper.JsonMapper._

  implicit val jsonFormat =
    Json.using[Json.WithDefaultValues].format[UserView]
}

@Singleton
class UserViewService @Inject()(userViewDao: UserViewDao) extends LazyLogging {

  val constructView = Sink.foreach[Event] { event =>
    event.`type` match {
      case EventType.USER_PROFILE_CREATED_OR_UPDATED =>
        val userView = event.data.as[UserView]
        userViewDao
          .findByEmail(userView.email)
          .map { userViews =>
            val targetUserView = if (userViews.nonEmpty) {
              userViews.head
            } else {
              userView
            }

            userViewDao.upsert(targetUserView)
          }
      case EventType.USER_SCHEMA_INIT =>
        userViewDao.init()
    }
  }

  def findAll() = {
    userViewDao.findAll()
  }
}

@Singleton
class UserViewDao {

  import slick.jdbc.H2Profile.api._

  class UserViewTable(tag: Tag) extends Table[UserView](tag, "USER_VIEW") {
    def id = column[Long]("ID", O.PrimaryKey, O.AutoInc)
    def name = column[String]("NAME")
    def email = column[String]("EMAIL", O.Unique)

    def * =
      (id.?, name, email) <> ((UserView.apply _).tupled, UserView.unapply)
  }

  private val userViewTable = TableQuery[UserViewTable]

  private val db = Database.forConfig("h2")

  def init() = {
    db.run(userViewTable.schema.create)
  }

  def upsert(userView: UserView) = {
    db.run(userViewTable.insertOrUpdate(userView))
  }

  def findByEmail(email: String): Future[Seq[UserView]] = {
    db.run(
      userViewTable
        .filter(userView => userView.email === email)
        .result
    )
  }

  def findAll(): Future[Seq[UserView]] = {
    db.run(userViewTable.result)
  }
}
