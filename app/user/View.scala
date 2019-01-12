package user

import java.util.UUID

import akka.stream.scaladsl.Sink
import com.typesafe.scalalogging.LazyLogging
import javax.inject.{Inject, Singleton}
import play.api.libs.json.Json
import event.{Event, EventType}
import utils.db.{Dao, ViewDatabase}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

case class UserView(uuid: Option[UUID] = Some(UUID.randomUUID()),
                    name: String,
                    email: String)

object UserView {

  implicit val jsonFormat =
    Json.using[Json.WithDefaultValues].format[UserView]

  val tableName = "user_view"
  val uuidName = "uuid"
  val nameName = "name"
  val emailName = "email"
}

@Singleton
class UserViewService @Inject()(userViewDao: UserViewDao) extends LazyLogging {

  def upsert(userView: UserView) = {
    userViewDao.upsert(userView)
  }

  def findByEmail(email: String) = {
    userViewDao.findByEmail(email)
  }

  def findAll() = {
    userViewDao.findAll()
  }

  def evolve(targetVersion: String) = {
    userViewDao.evolve(targetVersion)
  }
}

@Singleton
class UserViewDao extends Dao with LazyLogging {

  import utils.db.PostgresProfile.api._

  class UserViewTable(tag: Tag)
      extends Table[UserView](tag, UserView.tableName) {
    def uuid = column[UUID](UserView.uuidName, O.PrimaryKey)
    def name = column[String](UserView.nameName)
    def email = column[String](UserView.emailName)

    def * =
      (uuid.?, name, email) <> ((UserView.apply _).tupled, UserView.unapply)
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
          CREATE TABLE #${UserView.tableName}(
            #${UserView.uuidName} UUID PRIMARY KEY DEFAULT gen_random_uuid(),
            #${UserView.nameName} TEXT DEFAULT '' NOT NULL,
            #${UserView.emailName} TEXT UNIQUE DEFAULT '' NOT NULL
          )
        """)
      case _ =>
        logger.error(s"No such versioning defined with $targetVersion")
    }
  }
}
