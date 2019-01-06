package src.menu

import akka.stream.scaladsl.Sink
import com.typesafe.scalalogging.LazyLogging
import javax.inject.{Inject, Singleton}
import monocle.macros.GenLens
import play.api.libs.json._
import src.utils.ViewDatabase
import src.{Event, EventType}
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

case class MenuView(id: Option[Long] = None,
                    name: String = "NONE",
                    ingredients: Seq[String] = Seq(""),
                    recipe: String = "NONE",
                    link: String = "NONE",
                    selectedCount: Int = 0)

object MenuView {
  import src.utils.mapper.JsonMapper._

  implicit val jsonFormatter =
    Json.using[Json.WithDefaultValues].format[MenuView]
}

class MenuViewService @Inject()(viewDatabase: ViewDatabase,
                                menuViewDao: MenuViewDao) {

  val constructView = Sink.foreach[Event] { event =>
    event.`type` match {
      case EventType.RANDOM_MENU_ASKED =>
        val menuView = event.data.as[MenuView]
        menuViewDao
          .findByName(menuView.name)
          .map { menuViews =>
            val targetMenuView = if (menuViews.nonEmpty) {
              menuViews.head
            } else {
              menuView
            }

            val modifiedMenuView =
              GenLens[MenuView](_.selectedCount).modify(_ + 1)(targetMenuView)
            menuViewDao.upsert(modifiedMenuView)
          }
      case EventType.MENU_PROFILE_CREATED_OR_UPDATED =>
        val menuView = event.data.as[MenuView]
        menuViewDao
          .findByName(menuView.name)
          .map { menuViews =>
            val targetMenuView = if (menuViews.nonEmpty) {
              menuViews.head
            } else {
              menuView
            }

            menuViewDao.upsert(targetMenuView)
          }
      case EventType.MENU_SCHEMA_EVOLVED =>
        viewDatabase.evolveViewSchema(event)(
          targetVersion => menuViewDao.evolve(targetVersion)
        )
    }
  }

  def findAll(): Future[Seq[MenuView]] = {
    menuViewDao.findAll()
  }
}

@Singleton
class MenuViewDao extends LazyLogging {

  import slick.jdbc.H2Profile.api._
  import src.utils.mapper.ObjectRelationalMapper._

  class MenuViewTable(tag: Tag) extends Table[MenuView](tag, "MENU_VIEW") {
    def id = column[Long]("ID", O.PrimaryKey, O.AutoInc)
    def name = column[String]("NAME", O.Unique)
    def ingredients = column[Seq[String]]("INGREDIENTS")
    def recipe = column[String]("RECIPE")
    def link = column[String]("LINK")
    def selectedCount = column[Int]("SELECTED_COUNT")

    def * =
      (id.?, name, ingredients, recipe, link, selectedCount) <> ((MenuView.apply _).tupled, MenuView.unapply)
  }

  private val menuViewTable = TableQuery[MenuViewTable]

  private val db = Database.forConfig("h2")

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
          create table MENU_VIEW(
            ID bigint not null auto_increment primary key,
            NAME varchar not null,
            INGREDIENTS varchar not null,
            RECIPE varchar not null,
            LINK varchar not null,
            SELECTED_COUNT int not null
          )
        """)
      case _ =>
        logger.error(s"No such versioning defined with $targetVersion")
    }
  }
}
