package src.user

import akka.stream.scaladsl.Sink

case class UserEvent(id: Option[Long],
                     timestamp: Option[Long],
                     typ: String,
                     data: String)

object UserEventService {

  val storeEvent = Sink.foreach[UserEvent] { userEvent =>
    UserEventRepository.insert(userEvent)
  }
}

object UserEventRepository {

  import slick.jdbc.H2Profile.api._

  class UserEventTable(tag: Tag) extends Table[UserEvent](tag, "USER_EVENT") {
    def id = column[Long]("ID", O.PrimaryKey, O.AutoInc)
    def timeStamp = column[Long]("TIME_STAMP")
    def typ = column[String]("TYPE")
    def data = column[String]("DATA")

    def * =
      (id.?, timeStamp.?, typ, data) <> (UserEvent.tupled, UserEvent.unapply)
  }

  private val userEventTable = TableQuery[UserEventTable]

  private val db = Database.forConfig("h2")

  db.run(userEventTable.schema.create)

  def insert(userEvent: UserEvent) = {
    db.run(userEventTable += userEvent)
  }

  def findAll() = {
    db.run(userEventTable.result)
  }
}
