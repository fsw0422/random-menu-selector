package user

import java.util.UUID

import cats.effect.IO
import javax.inject.{Inject, Singleton}
import play.api.libs.json.Json
import slick.basic.DatabaseConfig
import slick.jdbc.JdbcProfile
import utils.GenericToolset

import scala.concurrent.Future

final case class UserView(
  uuid: UUID,
  name: Option[String],
  email: Option[String]
)

object UserView {

  implicit val jsonFormat = Json.format[UserView]
}

@Singleton
class UserViewHandler @Inject()(
  genericToolset: GenericToolset,
  userViewDao: UserViewDao
) {

  def createOrUpdate(user: User): IO[Int] = {
    val newUserView = user.uuid.fold {
      UserView(
        uuid = genericToolset.randomUUID(),
        email = user.email,
        name = user.name
      )
    } { userUuid =>
      UserView(
        uuid = userUuid,
        email = user.email,
        name = user.name
      )
    }
    IO.fromFuture(IO(userViewDao.upsert(newUserView)))
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
      (uuid, name.?, email.?) <> ((UserView.apply _).tupled, UserView.unapply)
  }

  private lazy val viewTable = TableQuery[UserViewTable]

  private lazy val db = DatabaseConfig.forConfig[JdbcProfile]("postgres").db

  def upsert(userView: UserView): Future[Int] = db.run {
    viewTable.insertOrUpdate(userView)
  }

  def findByUuid(uuid: UUID): Future[Option[UserView]] = db.run {
    viewTable
      .filter(userView => userView.uuid === uuid)
      .result
      .headOption
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