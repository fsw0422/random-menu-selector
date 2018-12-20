package src.menu

import akka.stream.scaladsl.Sink

case class MenuEvent(id: Option[Long],
                     timestamp: Option[Long],
                     typ: String,
                     data: String)

object MenuEventService {

  val storeEvent = Sink.foreach[MenuEvent] { menuEvent =>
    MenuEventRepository.insert(menuEvent)
  }
}

object MenuEventRepository {

  import slick.jdbc.H2Profile.api._

  class MenuEventTable(tag: Tag) extends Table[MenuEvent](tag, "MENU_EVENT") {
    def id = column[Long]("ID", O.PrimaryKey, O.AutoInc)
    def timeStamp = column[Long]("TIME_STAMP")
    def typ = column[String]("TYPE")
    def data = column[String]("DATA")

    def * =
      (id.?, timeStamp.?, typ, data) <> (MenuEvent.tupled, MenuEvent.unapply)
  }

  private val menuEventTable = TableQuery[MenuEventTable]

  private val db = Database.forConfig("h2")

  db.run(menuEventTable.schema.create)

  def insert(menuEvent: MenuEvent) = {
    db.run(menuEventTable += menuEvent)
  }

  def findAll() = {
    db.run(menuEventTable.result)
  }
}
