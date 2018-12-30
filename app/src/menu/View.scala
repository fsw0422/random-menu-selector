package src.menu

import akka.actor.ActorSystem
import akka.stream.scaladsl.Sink
import com.typesafe.scalalogging.LazyLogging
import monocle.macros.GenLens
import play.api.libs.json._
import src.EventType.EventType
import src.{Event, EventDao, EventType}

import scala.concurrent.Future

case class MenuView(id: Option[Long] = None,
                    name: String = "NONE",
                    ingredients: Seq[String] = Seq(""),
                    recipe: String = "NONE",
                    link: String = "NONE",
                    selectedCount: Int = 0)

object MenuView {
  import src.utils.mapper.JsMapper._

  implicit val jsonFormatter =
    Json.using[Json.WithDefaultValues].format[MenuView]
}

object MenuViewService {

  val constructView = Sink.foreach[Event] { event =>
    event.`type` match {
      case EventType.RANDOM_MENU_ASKED =>
        val menuView = event.data.as[MenuView]
        val modifiedMenuView =
          GenLens[MenuView](_.selectedCount).modify(_ + 1)(menuView)
        MenuViewDao.upsert(modifiedMenuView)
      case EventType.MENU_PROFILE_CREATED_OR_UPDATED =>
        val menuView = event.data.as[MenuView]
        MenuViewDao.upsert(menuView)
      case EventType.MENU_SCHEMA_CREATED_OR_UPDATED =>
        val targetVersion = (event.data \ "version").as[String]
        MenuViewDao.evolve(event.`type`, targetVersion)
    }
  }

  def findAll(): Future[Seq[MenuView]] = {
    MenuViewDao.findAll()
  }
}

object MenuViewDao extends LazyLogging {

  import slick.jdbc.H2Profile.api._
  import src.utils.mapper.H2TypeMapper._

  private implicit val actorSystem = ActorSystem("MenuViewDao")
  private implicit val executionContext = actorSystem.dispatcher

  class MenuViewTable(tag: Tag) extends Table[MenuView](tag, "MENU_VIEW") {
    def id = column[Long]("ID", O.PrimaryKey, O.AutoInc)
    def name = column[String]("NAME")
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

  def findAll(): Future[Seq[MenuView]] = {
    db.run(menuViewTable.result)
  }

  def evolve(eventType: EventType, targetVersion: String) = {
    EventDao.findByType(eventType).map { events =>
      val sameVersionExists = events
        .exists(event => (event.data \ "version").as[String] == targetVersion)
      if (!sameVersionExists) {
        targetVersion match {
          case "1.0" =>
            db.run(menuViewTable.schema.create)
          case _ =>
            logger.warn(s"No version change is defined with $targetVersion")
        }
      } else {
        logger.warn(s"Event with $targetVersion already exists")
      }
    }
  }
}
