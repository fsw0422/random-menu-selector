package user

import java.util.UUID

import cats.effect.IO
import com.typesafe.scalalogging.LazyLogging
import javax.inject.{Inject, Singleton}
import play.api.libs.json.Json
import utils.db.Db

case class UserView(uuid: Option[UUID] = Some(UUID.randomUUID()),
                    name: String,
                    email: String)

object UserView {

  implicit val jsonFormat = Json
    .using[Json.WithDefaultValues]
    .format[UserView]

  val tableName = "user_view"
  val uuidColumn = "uuid"
  val nameColumn = "name"
  val emailColumn = "email"
}

@Singleton
class UserViewService @Inject()(userViewDao: UserViewDao) {

  def upsert(userView: UserView): IO[Unit] = {
    userViewDao.upsert(userView)
  }

  def findByEmail(email: String): IO[Seq[UserView]] = {
    userViewDao.findByEmail(email)
  }

  def findAll(): IO[Seq[UserView]] = {
    userViewDao.findAll()
  }

  def delete(uuid: UUID): IO[Unit] = {
    userViewDao.delete(uuid)
  }

  def evolve(targetVersion: String): IO[Unit] = {
    userViewDao.evolve(targetVersion)
  }
}

@Singleton
class UserViewDao extends Db with LazyLogging {

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

  override def setup(): IO[Unit] = IO.fromFuture {
    IO {
      db.run(sqlu"""CREATE TABLE IF NOT EXISTS #${UserView.tableName}()""")
        .map(_ => ())
    }
  }

  override def teardown(): IO[Unit] = IO.fromFuture {
    IO(db.run(userViewTable.schema.drop))
  }

  def upsert(userView: UserView): IO[Unit] = IO.fromFuture {
    IO{
      db.run(userViewTable.insertOrUpdate(userView))
        .map(_ => ())
    }
  }

  def findByEmail(email: String): IO[Seq[UserView]] = IO.fromFuture {
    IO {
      db.run {
        userViewTable
          .filter(userView => userView.email === email)
          .result
      }
    }
  }

  def findAll(): IO[Seq[UserView]] = IO.fromFuture {
    IO(db.run(userViewTable.result))
  }

  def delete(uuid: UUID): IO[Unit] = IO.fromFuture {
    IO {
      db.run {
        userViewTable
          .filter(menuView => menuView.uuid === uuid)
          .delete
      }.map(_ => ())
    }
  }

  def evolve(targetVersion: String): IO[Unit] = IO.fromFuture {
    IO {
      targetVersion match {
        case "1.0" =>
          db.run(
            DBIO.seq(
              sqlu"""CREATE EXTENSION IF NOT EXISTS "pgcrypto"""",
              sqlu"""ALTER TABLE #${UserView.tableName} ADD COLUMN #${UserView.uuidColumn} UUID PRIMARY KEY DEFAULT gen_random_uuid()""",
              sqlu"""ALTER TABLE #${UserView.tableName} ADD COLUMN #${UserView.nameColumn} TEXT DEFAULT '' NOT NULL""",
              sqlu"""ALTER TABLE #${UserView.tableName} ADD COLUMN #${UserView.emailColumn} TEXT UNIQUE DEFAULT '' NOT NULL"""
            )
          )
        case "2.0" =>
          db.run(
            DBIO.seq(
              sqlu"""ALTER TABLE #${UserView.tableName} ALTER COLUMN #${UserView.uuidColumn} DROP DEFAULT""",
              sqlu"""ALTER TABLE #${UserView.tableName} ALTER COLUMN #${UserView.nameColumn} DROP DEFAULT""",
              sqlu"""ALTER TABLE #${UserView.tableName} ALTER COLUMN #${UserView.emailColumn} DROP DEFAULT"""
            )
          )
        case "3.0" =>
          db.run(sqlu"""DROP EXTENSION IF EXISTS "pgcrypto"""").map(_ => ())
      }
    }
  }
}
