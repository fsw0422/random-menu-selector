package src.menu

import akka.stream.scaladsl.Sink

case class MenuView(id: Option[Long],
                    createdTime: Option[Long],
                    updatedTime: Option[Long],
                    name: String,
                    ingredients: String,
                    recipe: String,
                    link: String)

object MenuViewService {

  val constructView = Sink.foreach[MenuEvent] { menuEvent =>
    //TODO: construct view out of event and store toe
  }

  def findAll() = {
    MenuViewRepository.findAll()
  }
}

object MenuViewRepository {

  import slick.jdbc.H2Profile.api._

  class MenuViewTable(tag: Tag) extends Table[MenuView](tag, "MENU_VIEW") {
    def id = column[Long]("ID", O.PrimaryKey, O.AutoInc)
    def createdTime = column[Long]("CREATED_TIME")
    def updatedTime = column[Long]("UPDATED_TIME")
    def name = column[String]("NAME")
    def ingredients = column[String]("INGREDIENTS")
    def recipe = column[String]("RECIPE")
    def link = column[String]("LINK")

    def * =
      (id.?, createdTime.?, updatedTime.?, name, ingredients, recipe, link) <> (MenuView.tupled, MenuView.unapply)
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
