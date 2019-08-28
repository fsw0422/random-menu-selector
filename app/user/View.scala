package user

import java.util.UUID

import cats.effect.IO
import com.typesafe.scalalogging.LazyLogging
import javax.inject.Singleton
import play.api.libs.json.Json
import utils.db.Db

final case class UserView(
  uuid: Option[UUID] = Some(UUID.randomUUID()),
  name: String,
  email: String
)

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
    IO { db.run(userViewTable.schema.drop) }
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
    IO { db.run(userViewTable.result) }
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
}
