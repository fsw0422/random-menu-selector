package src.menu

import akka.stream.scaladsl.Sink
import play.api.libs.json.Json

object MenuView {
  implicit val jsonFormat = Json.format[MenuView]
}

case class MenuView(id: Option[Long],
                    timeStamp: Long,
                    name: String,
                    ingredients: List[String],
                    recipe: String,
                    link: String)

object MenuViewService {

  val constructView = Sink.foreach[MenuEvent] { menuEvent =>
    //TODO: construct view out of event and store
  }

  def findAll(): Unit = {
    MenuViewRepository.findAll()
  }
}

object MenuViewRepository {

  import slick.jdbc.H2Profile.api._

  class MenuViewTable(tag: Tag) extends Table[MenuView](tag, "MENU_VIEW") {
    def id = column[Long]("ID", O.PrimaryKey, O.AutoInc)
    def timeStamp = column[Long]("TIME_STAMP")
    def name = column[String]("NAME")
    def ingredients = column[Seq[String]]("INGREDIENTS")
    def recipe = column[String]("RECIPE")
    def link = column[String]("LINK")

    def * =
      (id.?, timeStamp, name, ingredients, recipe, link) <> (MenuView.tupled, MenuView.unapply)
  }

  private val menuViewTable = TableQuery[MenuViewTable]

  private val db = Database.forConfig("h2")

  db.run(menuViewTable.schema.create)

  def insert(menuView: MenuView) = {
    db.run(menuViewTable += menuView)
  }

  def findAll() = {
    db.run(menuViewTable.result)
  }
}
