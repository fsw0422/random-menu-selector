package src.user

import akka.stream.scaladsl.Sink
import com.typesafe.scalalogging.LazyLogging
import javax.inject.{Inject, Singleton}
import monocle.macros.GenLens
import play.api.libs.json.Json
import src.utils.ViewDatabase
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
class UserViewService @Inject()(viewDatabase: ViewDatabase,
                                userViewDao: UserViewDao) {

  val constructView = Sink.foreach[Event] { event =>
    event.`type` match {
      case EventType.USER_PROFILE_CREATED_OR_UPDATED =>
        val userView = event.data.as[UserView]
        userViewDao
          .findByEmail(userView.email)
          .map { userViews =>
            val targetUserView = if (userViews.nonEmpty) {
              val modifiedUserView = userViews.head
              val lens = GenLens[UserView]
              lens(_.name)
                .modify(name => modifiedUserView.name)(modifiedUserView)
              lens(_.email)
                .modify(email => modifiedUserView.email)(modifiedUserView)
            } else {
              userView
            }

            userViewDao.upsert(targetUserView)
          }
      case EventType.USER_SCHEMA_EVOLVED =>
        viewDatabase.evolveViewSchema(event)(
          targetVersion => userViewDao.evolve(targetVersion)
        )
    }
  }

  def findAll() = {
    userViewDao.findAll()
  }
}

@Singleton
class UserViewDao extends LazyLogging {

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

  def evolve(targetVersion: String) = {
    targetVersion match {
      case "1.0" =>
        db.run(sqlu"""
          create table USER_VIEW(
            ID bigint not null auto_increment primary key,
            NAME varchar not null,
            EMAIL varchar not null
          )
        """)
      case _ =>
        logger.error(s"No such versioning defined with $targetVersion")
    }
  }
}
