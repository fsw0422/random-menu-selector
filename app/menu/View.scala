package menu

import java.util.UUID

import cats.effect.IO
import com.typesafe.scalalogging.LazyLogging
import javax.inject.Singleton
import play.api.libs.json._
import utils.db.Db

final case class Menu(
  uuid: Option[UUID] = Some(UUID.randomUUID()),
  name: String,
  ingredients: Seq[String],
  recipe: String,
  link: String,
  selectedCount: Option[Int] = Some(0)
)

object Menu {

  implicit val jsonFormatter = Json
    .using[Json.WithDefaultValues]
    .format[Menu]
}

@Singleton
class MenuViewDao extends Db with LazyLogging {

  import utils.db.PostgresProfile.api._

  class MenuViewTable(tag: Tag)
      extends Table[Menu](tag, "menu_view") {
    def uuid = column[UUID]("uuid", O.PrimaryKey)
    def name = column[String]("name")
    def ingredients = column[Seq[String]]("ingredients")
    def recipe = column[String]("recipe")
    def link = column[String]("link")
    def selectedCount = column[Int]("selected_count")

    def * =
      (uuid.?, name, ingredients, recipe, link, selectedCount.?) <> ((Menu.apply _).tupled, Menu.unapply)
  }

  private val menuViewTable = TableQuery[MenuViewTable]

  override def setup(): IO[Unit] = IO.fromFuture {
    IO {
      db.run(sqlu"""CREATE TABLE IF NOT EXISTS #menu_view()""")
        .map(_ => ())
    }
  }

  override def teardown(): IO[Unit] = IO.fromFuture {
    IO { db.run(menuViewTable.schema.drop) }
  }

  def upsert(menuView: Menu): IO[Int] = IO.fromFuture {
    IO { db.run(menuViewTable.insertOrUpdate(menuView)) }
  }

  def findByName(name: String): IO[Seq[Menu]] = IO.fromFuture {
    IO {
      db.run {
        menuViewTable
          .filter(menuView => menuView.name === name)
          .result
      }
    }
  }

  def findByNameLike(name: String): IO[Seq[Menu]] = IO.fromFuture {
    IO {
      db.run {
        menuViewTable
          .filter(menuView => menuView.name like "%" + name + "%")
          .result
      }
    }
  }

  def findAll(): IO[Seq[Menu]] = IO.fromFuture {
    IO { db.run(menuViewTable.result) }
  }

  def delete(uuid: UUID): IO[Unit] = IO.fromFuture {
    IO {
      db.run {
        menuViewTable
          .filter(menuView => menuView.uuid === uuid)
          .delete
      }.map(_ => ())
    }
  }
}
