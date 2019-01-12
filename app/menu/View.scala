package menu

import java.util.UUID

import akka.stream.scaladsl.Sink
import com.typesafe.scalalogging.LazyLogging
import javax.inject.{Inject, Singleton}
import play.api.libs.json._
import event.{Event, EventType}
import utils.db.{Dao, ViewDatabase}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

case class MenuView(uuid: Option[UUID] = Some(UUID.randomUUID()),
                    name: String,
                    ingredients: Seq[String],
                    recipe: String,
                    link: String,
                    selectedCount: Int)

object MenuView {

  implicit val jsonFormatter =
    Json.using[Json.WithDefaultValues].format[MenuView]

  val tableColumn = "menu_view"
  val uuidColumn = "uuid"
  val nameColumn = "name"
  val ingredientsColumn = "ingredients"
  val recipeColumn = "recipe"
  val linkColumn = "link"
  val selectedCountColumn = "selected_count"
}

@Singleton
class MenuViewService @Inject()(menuViewDao: MenuViewDao) extends LazyLogging {

  def upsert(menuView: MenuView) = {
    menuViewDao.upsert(menuView)
  }

  def findByName(name: String): Future[Seq[MenuView]] = {
    menuViewDao.findByName(name)
  }

  def findAll(): Future[Seq[MenuView]] = {
    menuViewDao.findAll()
  }

  def evolve(targetVersion: String) = {
    menuViewDao.evolve(targetVersion)
  }
}

@Singleton
class MenuViewDao extends Dao with LazyLogging {

  import utils.db.PostgresProfile.api._

  class MenuViewTable(tag: Tag)
      extends Table[MenuView](tag, MenuView.tableColumn) {
    def uuid = column[UUID](MenuView.uuidColumn, O.PrimaryKey)
    def name = column[String](MenuView.nameColumn)
    def ingredients = column[Seq[String]](MenuView.ingredientsColumn)
    def recipe = column[String](MenuView.recipeColumn)
    def link = column[String](MenuView.linkColumn)
    def selectedCount = column[Int](MenuView.selectedCountColumn)

    def * =
      (uuid.?, name, ingredients, recipe, link, selectedCount) <> ((MenuView.apply _).tupled, MenuView.unapply)
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
          CREATE TABLE #${MenuView.tableColumn}(
            #${MenuView.uuidColumn} UUID PRIMARY KEY DEFAULT gen_random_uuid(),
            #${MenuView.nameColumn} TEXT UNIQUE NOT NULL DEFAULT '',
            #${MenuView.ingredientsColumn} TEXT[] NOT NULL DEFAULT '{}',
            #${MenuView.recipeColumn} TEXT NOT NULL DEFAULT '',
            #${MenuView.linkColumn} TEXT NOT NULL DEFAULT '',
            #${MenuView.selectedCountColumn} INTEGER NOT NULL DEFAULT 0
          )
        """)
      case _ =>
        logger.error(s"No such versioning defined with $targetVersion")
    }
  }
}
