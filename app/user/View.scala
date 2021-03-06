package user

import java.util.UUID

import cats.effect.IO
import com.typesafe.scalalogging.LazyLogging
import javax.inject.{Inject, Singleton}
import play.api.libs.json.Json
import slick.basic.DatabaseConfig
import slick.jdbc.JdbcProfile

import scala.concurrent.Future

final case class UserView(
  uuid: Option[UUID] = Some(UUID.randomUUID()),
  name: Option[String],
  email: Option[String]
)

object UserView {

  implicit val jsonFormat = Json.format[UserView]
}

@Singleton
class UserViewHandler @Inject()(userViewDao: UserViewDao) extends LazyLogging {

  def createOrUpdate(user: User): IO[Int] = {
    user.uuid.fold {
      logger.error("UUID should not be None in UserViewHandler.CreateOrUpdate")
      IO.pure(0)
    } { userUuid =>
      for {
        userViewOpt <- IO.fromFuture(IO(userViewDao.findByUuid(userUuid)))
        createdOrUpdatedUserView = userViewOpt.fold(
          UserView(
            email = user.email,
            name = user.name
          )
        )(userView =>
          userView.copy(
            email = userView.email.fold(userView.email)(email => Some(email)),
            name = userView.name.fold(userView.name)(name => Some(name))
          )
        )
        affectedRowNum <- IO.fromFuture(IO(userViewDao.upsert(createdOrUpdatedUserView)))
      } yield affectedRowNum
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
      (uuid.?, name.?, email.?) <> ((UserView.apply _).tupled, UserView.unapply)
  }

  private lazy val viewTable = TableQuery[UserViewTable]

  private lazy val db = DatabaseConfig.forConfig[JdbcProfile]("postgres").db

  def upsert(userView: UserView): Future[Int] = db.run {
    userView.uuid.fold(
      viewTable
        .map(t => (t.name.?, t.email.?))
        .insertOrUpdate((userView.name, userView.email))
    )(uuid => viewTable.insertOrUpdate(userView))
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