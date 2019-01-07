package src.user

import java.util.UUID
import akka.stream.scaladsl.Sink
import com.typesafe.scalalogging.LazyLogging
import javax.inject.{Inject, Singleton}
import play.api.libs.json.Json
import src.utils.{Dao, ViewDatabase}
import src.{Event, EventType}
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

case class UserView(uuid: UUID, name: String, email: String)

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
      case EventType.USER_SCHEMA_EVOLVED =>
        viewDatabase.evolveViewSchema(event)(
          targetVersion => userViewDao.evolve(targetVersion)
        )
    }
  }

  def upsert(userView: UserView) = {
    userViewDao.upsert(userView)
  }

  def findByEmail(email: String) = {
    userViewDao.findByEmail(email)
  }

  def findAll() = {
    userViewDao.findAll()
  }
}

@Singleton
class UserViewDao extends Dao with LazyLogging {

  import src.utils.mapper.OrmMapper.api._

  val tableName = "user_view"
  val uuidName = "uuid"
  val nameName = "name"
  val emailName = "email"

  class UserViewTable(tag: Tag) extends Table[UserView](tag, tableName) {
    def uuid =
      column[UUID](uuidName, O.PrimaryKey, O.Default(UUID.randomUUID()))
    def name = column[String](nameName, O.Default("NONE"))
    def email = column[String](emailName, O.Unique, O.Default("NONE"))

    def * =
      (uuid, name, email) <> ((UserView.apply _).tupled, UserView.unapply)
  }

  private val userViewTable = TableQuery[UserViewTable]

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
          CREATE TABLE $tableName(
            $uuidName UUID PRIMARY KEY,
            $nameName TEXT NOT NULL,
            $emailName TEXT UNIQUE NOT NULL
          )
        """)
      case _ =>
        logger.error(s"No such versioning defined with $targetVersion")
    }
  }
}
