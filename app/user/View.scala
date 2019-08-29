package user

import java.util.UUID

import cats.effect.IO
import com.typesafe.scalalogging.LazyLogging
import javax.inject.Singleton
import utils.db.Db

@Singleton
class UserViewDao extends Db with LazyLogging {

  import utils.db.PostgresProfile.api._

  val tableName = "user_view"

  class UserViewTable(tag: Tag)
    extends Table[User](tag, tableName) {
    def uuid = column[UUID]("uuid", O.PrimaryKey)
    def name = column[String]("name")
    def email = column[String]("email")

    def * =
      (uuid.?, name, email) <> ((User.apply _).tupled, User.unapply)
  }

  private val userViewTable = TableQuery[UserViewTable]

  override def setup(): IO[Unit] = IO.fromFuture {
    IO { db.run(userViewTable.schema.create) }
  }

  override def teardown(): IO[Unit] = IO.fromFuture {
    IO { db.run(userViewTable.schema.drop) }
  }

  def upsert(userView: User): IO[Int] = IO.fromFuture {
    IO { db.run(userViewTable.insertOrUpdate(userView)) }
  }

  def findByEmail(email: String): IO[Seq[User]] = IO.fromFuture {
    IO {
      db.run {
        userViewTable
          .filter(userView => userView.email === email)
          .result
      }
    }
  }

  def findAll(): IO[Seq[User]] = IO.fromFuture {
    IO { db.run(userViewTable.result) }
  }

  def delete(uuid: UUID): IO[Int] = IO.fromFuture {
    IO {
      db.run {
        userViewTable
          .filter(menuView => menuView.uuid === uuid)
          .delete
      }
    }
  }
}
