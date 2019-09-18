package user

import java.util.UUID

import cats.effect.IO
import javax.inject.{Inject, Singleton}
import play.api.libs.json.Json
import slick.basic.DatabaseConfig
import slick.jdbc.JdbcProfile

import scala.concurrent.Future

final case class UserView(
  uuid: UUID,
  name: String,
  email: String
)

object UserView {

  implicit val jsonFormat = Json
    .using[Json.WithDefaultValues]
    .format[UserView]
}

@Singleton
class UserViewHandler @Inject()(userViewDao: UserViewDao) {

  def create(user: User): IO[Int] = {
    user.uuid.fold(IO.pure(0)) { userUuid =>
      val newMenuView = UserView(
        uuid = userUuid,
        email = user.email.getOrElse(""),
        name = user.name.getOrElse("")
      )
      IO.fromFuture(IO(userViewDao.insert(newMenuView)))
    }
  }

  def update(user: User): IO[Int] = {
    user.uuid.fold(IO.pure(0)) { userUuid =>
      IO.fromFuture(IO(userViewDao.findByUuid(userUuid))).map { userViews =>
        userViews.headOption.fold(0) { userView =>
          val newUserView = userView.copy(
            uuid = userUuid,
            name = user.name.getOrElse(userView.name),
            email = user.email.getOrElse(userView.email)
          )
          IO.fromFuture(IO(userViewDao.update(newUserView))).unsafeRunSync()
        }
      }
    }
  }

  def delete(uuid: UUID): IO[Int] = {
    IO.fromFuture(IO(userViewDao.delete(uuid)))
  }

  def searchAll(): IO[Seq[UserView]] = {
    IO.fromFuture(IO(userViewDao.findAll()))
  }
}

@Singleton
class UserViewDao {

  import utils.db.PostgresProfile.api._

  class UserViewTable(tag: Tag) extends Table[UserView](tag, "user_view") {

    def uuid = column[UUID]("uuid", O.PrimaryKey)
    def name = column[String]("name")
    def email = column[String]("email")

    def * =
      (uuid, name, email) <> ((UserView.apply _).tupled, UserView.unapply)
  }

  private lazy val viewTable = TableQuery[UserViewTable]

  private lazy val db = DatabaseConfig.forConfig[JdbcProfile]("postgres").db

  def insert(userView: UserView): Future[Int] = db.run {
    viewTable += userView
  }

  def update(userView: UserView): Future[Int] = db.run {
    viewTable.update(userView)
  }

  def findByUuid(uuid: UUID): Future[Seq[UserView]] = db.run {
    viewTable
      .filter(userView => userView.uuid === uuid)
      .result
  }

  def findAll(): Future[Seq[UserView]] = db.run {
    viewTable.result
  }

  def delete(uuid: UUID): Future[Int] = db.run {
    viewTable
      .filter(menuView => menuView.uuid === uuid)
      .delete
  }
}