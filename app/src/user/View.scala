package src.user

import akka.stream.scaladsl.Sink

case class UserView(id: Option[Long],
                    createdTime: Option[Long],
                    updatedTime: Option[Long],
                    name: String,
                    email: String)

object UserViewService {

  val constructView = Sink.foreach[UserEvent] { userEvent =>
    //TODO: construct view out of event and store
  }

  def findAll() = {
    UserViewRepository.findAll()
  }
}

object UserViewRepository {

  import slick.jdbc.H2Profile.api._

  class UserViewTable(tag: Tag) extends Table[UserView](tag, "USER_VIEW") {
    def id = column[Long]("ID", O.PrimaryKey, O.AutoInc)
    def createdTime = column[Long]("CREATED_TIME")
    def updatedTime = column[Long]("UPDATED_TIME")
    def name = column[String]("NAME")
    def email = column[String]("EMAIL")

    def * =
      (id.?, createdTime.?, updatedTime.?, name, email) <> (UserView.tupled, UserView.unapply)
  }

  private val userViewTable = TableQuery[UserViewTable]

  private val db = Database.forConfig("h2")

  db.run(userViewTable.schema.create)

  def insert(userView: UserView) = {
    db.run(userViewTable += userView)
  }

  def findAll() = {
    db.run(userViewTable.result)
  }
}
