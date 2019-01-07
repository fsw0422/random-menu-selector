package src.menu

import java.util.UUID
import akka.stream.scaladsl.Sink
import com.typesafe.scalalogging.LazyLogging
import javax.inject.{Inject, Singleton}
import play.api.libs.json._
import src.utils.{Dao, ViewDatabase}
import src.{Event, EventType}
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

case class MenuView(uuid: UUID,
                    name: String,
                    ingredients: Seq[String],
                    recipe: String,
                    link: String,
                    selectedCount: Int)

object MenuView {
  import src.utils.mapper.JsonMapper._

  implicit val jsonFormatter =
    Json.using[Json.WithDefaultValues].format[MenuView]
}

class MenuViewService @Inject()(viewDatabase: ViewDatabase,
                                menuViewDao: MenuViewDao) {

  val constructView = Sink.foreach[Event] { event =>
    event.`type` match {
      case EventType.MENU_SCHEMA_EVOLVED =>
        viewDatabase.evolveViewSchema(event)(
          targetVersion => menuViewDao.evolve(targetVersion)
        )
    }
  }

  def upsert(menuView: MenuView) = {
    menuViewDao.upsert(menuView)
  }

  def findByName(name: String): Future[Seq[MenuView]] = {
    menuViewDao.findByName(name)
  }

  def findAll(): Future[Seq[MenuView]] = {
    menuViewDao.findAll()
  }
}

@Singleton
class MenuViewDao extends Dao with LazyLogging {

  import src.utils.mapper.OrmMapper.api._

  val tableColumn = "menu_view"
  val uuidColumn = "uuid"
  val nameColumn = "name"
  val ingredientsColumn = "ingredients"
  val recipeColumn = "recipe"
  val linkColumn = "link"
  val selectedCountColumn = "selected_count"

  class MenuViewTable(tag: Tag) extends Table[MenuView](tag, tableColumn) {
    def uuid =
      column[UUID](
        uuidColumn,
        O.PrimaryKey,
        O.AutoInc,
        O.Default(UUID.randomUUID())
      )
    def name = column[String]("name", O.Unique, O.Default("NONE"))
    def ingredients = column[Seq[String]](ingredientsColumn, O.Default(Seq("")))
    def recipe = column[String](recipeColumn, O.Default("NONE"))
    def link = column[String](linkColumn, O.Default("NONE"))
    def selectedCount = column[Int](selectedCountColumn, O.Default(0))

    def * =
      (uuid, name, ingredients, recipe, link, selectedCount) <> ((MenuView.apply _).tupled, MenuView.unapply)
  }

  private val menuViewTable = TableQuery[MenuViewTable]

  def upsert(menuView: MenuView) = {
    db.run(menuViewTable.insertOrUpdate(menuView))
  }

  def findByName(name: String): Future[Seq[MenuView]] = {
    db.run(
      menuViewTable
        .filter(menuView => menuView.name === name)
        .result
    )
  }

  def findAll(): Future[Seq[MenuView]] = {
    db.run(menuViewTable.result)
  }

  def evolve(targetVersion: String) = {
    targetVersion match {
      case "1.0" =>
        db.run(sqlu"""
          CREATE TABLE $tableColumn(
            $uuidColumn UUID PRIMARY KEY,
            $nameColumn TEXT UNIQUE NOT NULL,
            $ingredientsColumn TEXT[] NOT NULL,
            $recipeColumn TEXT NOT NULL,
            $linkColumn TEXT NOT NULL,
            $selectedCountColumn INTEGER NOT NULL
          )
        """)
      case _ =>
        logger.error(s"No such versioning defined with $targetVersion")
    }
  }
}
