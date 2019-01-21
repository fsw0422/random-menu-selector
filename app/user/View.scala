package user

import java.util.UUID

import com.typesafe.scalalogging.LazyLogging
import javax.inject.{Inject, Singleton}
import play.api.libs.json.Json
import utils.db.Dao

import scala.concurrent.Future

case class UserView(uuid: Option[UUID] = Some(UUID.randomUUID()),
                    name: String,
                    email: String)

object UserView {

  implicit val jsonFormat =
    Json.using[Json.WithDefaultValues].format[UserView]

  val tableName = "user_view"
  val uuidColumn = "uuid"
  val nameColumn = "name"
  val emailColumn = "email"
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
    def uuid = column[UUID](UserView.uuidColumn, O.PrimaryKey)
    def name = column[String](UserView.nameColumn)
    def email = column[String](UserView.emailColumn)

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
            #${UserView.uuidColumn} UUID PRIMARY KEY DEFAULT gen_random_uuid(),
            #${UserView.nameColumn} TEXT DEFAULT '' NOT NULL,
            #${UserView.emailColumn} TEXT UNIQUE DEFAULT '' NOT NULL
          )
        """)
      case "2.0" =>
        db.run(
          DBIO.seq(
            sqlu"""ALTER TABLE #${UserView.tableName} ALTER COLUMN #${UserView.uuidColumn} DROP DEFAULT""",
            sqlu"""ALTER TABLE #${UserView.tableName} ALTER COLUMN #${UserView.nameColumn} DROP DEFAULT""",
            sqlu"""ALTER TABLE #${UserView.tableName} ALTER COLUMN #${UserView.emailColumn} DROP DEFAULT"""
          )
        )
      case _ =>
        logger.error(s"No such versioning defined with $targetVersion")
    }
  }
}
