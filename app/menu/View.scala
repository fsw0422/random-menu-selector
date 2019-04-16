package menu

import java.util.UUID

import cats.effect.IO
import com.typesafe.scalalogging.LazyLogging
import javax.inject.{Inject, Singleton}
import play.api.libs.json._
import utils.db.Db

final case class MenuView(uuid: Option[UUID] = Some(UUID.randomUUID()),
                    name: String,
                    ingredients: Seq[String],
                    recipe: String,
                    link: String,
                    selectedCount: Option[Int] = Some(0))

object MenuView {

  implicit val jsonFormatter = Json
    .using[Json.WithDefaultValues]
    .format[MenuView]

  val tableName = "menu_view"
  val uuidColumn = "uuid"
  val nameColumn = "name"
  val ingredientsColumn = "ingredients"
  val recipeColumn = "recipe"
  val linkColumn = "link"
  val selectedCountColumn = "selected_count"
}

@Singleton
class MenuViewService @Inject()(menuViewDao: MenuViewDao) {

  def upsert(menuView: MenuView): IO[Unit] = {
    menuViewDao.upsert(menuView)
  }

  def findByName(name: String): IO[Seq[MenuView]] = {
    menuViewDao.findByName(name)
  }

  def findByNameLike(name: String): IO[Seq[MenuView]] = {
    menuViewDao.findByNameLike(name)
  }

  def findAll(): IO[Seq[MenuView]] = {
    menuViewDao.findAll()
  }

  def delete(uuid: UUID): IO[Unit] = {
    menuViewDao.delete(uuid)
  }

  def evolve(targetVersion: String): IO[Unit] = {
    menuViewDao.evolve(targetVersion)
  }
}

@Singleton
class MenuViewDao extends Db with LazyLogging {

  import utils.db.PostgresProfile.api._

  class MenuViewTable(tag: Tag)
      extends Table[MenuView](tag, MenuView.tableName) {
    def uuid = column[UUID](MenuView.uuidColumn, O.PrimaryKey)
    def name = column[String](MenuView.nameColumn)
    def ingredients = column[Seq[String]](MenuView.ingredientsColumn)
    def recipe = column[String](MenuView.recipeColumn)
    def link = column[String](MenuView.linkColumn)
    def selectedCount = column[Int](MenuView.selectedCountColumn)

    def * =
      (uuid.?, name, ingredients, recipe, link, selectedCount.?) <> ((MenuView.apply _).tupled, MenuView.unapply)
  }

  private val menuViewTable = TableQuery[MenuViewTable]

  override def setup(): IO[Unit] = IO.fromFuture {
    IO {
      db.run(sqlu"""CREATE TABLE IF NOT EXISTS #${MenuView.tableName}()""")
        .map(_ => ())
    }
  }

  override def teardown(): IO[Unit] = IO.fromFuture {
    IO(db.run(menuViewTable.schema.drop))
  }

  def upsert(menuView: MenuView): IO[Unit] = IO.fromFuture {
    IO {
      db.run(menuViewTable.insertOrUpdate(menuView))
        .map(_ => ())
    }
  }

  def findByName(name: String): IO[Seq[MenuView]] = IO.fromFuture {
    IO {
      db.run {
        menuViewTable
          .filter(menuView => menuView.name === name)
          .result
      }
    }
  }

  def findByNameLike(name: String): IO[Seq[MenuView]] = IO.fromFuture {
    IO {
      db.run {
        menuViewTable
          .filter(menuView => menuView.name like "%" + name + "%")
          .result
      }
    }
  }

  def findAll(): IO[Seq[MenuView]] = IO.fromFuture {
    IO(db.run(menuViewTable.result))
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

  def evolve(targetVersion: String): IO[Unit] = IO.fromFuture {
    IO {
      targetVersion match {
        case "1.0" =>
          db.run(
            DBIO.seq(
              sqlu"""CREATE EXTENSION IF NOT EXISTS "pgcrypto"""",
              sqlu"""ALTER TABLE #${MenuView.tableName} ADD COLUMN #${MenuView.uuidColumn} UUID PRIMARY KEY DEFAULT gen_random_uuid()""",
              sqlu"""ALTER TABLE #${MenuView.tableName} ADD COLUMN #${MenuView.nameColumn} TEXT UNIQUE NOT NULL DEFAULT ''""",
              sqlu"""ALTER TABLE #${MenuView.tableName} ADD COLUMN #${MenuView.ingredientsColumn} TEXT[] NOT NULL DEFAULT '{}'""",
              sqlu"""ALTER TABLE #${MenuView.tableName} ADD COLUMN #${MenuView.recipeColumn} TEXT NOT NULL DEFAULT ''""",
              sqlu"""ALTER TABLE #${MenuView.tableName} ADD COLUMN #${MenuView.linkColumn} TEXT NOT NULL DEFAULT ''""",
              sqlu"""ALTER TABLE #${MenuView.tableName} ADD COLUMN #${MenuView.selectedCountColumn} INTEGER NOT NULL DEFAULT 0""",
            )
          )
        case "2.0" =>
          db.run(
            DBIO.seq(
              sqlu"""ALTER TABLE #${MenuView.tableName} ALTER COLUMN #${MenuView.uuidColumn} DROP DEFAULT""",
              sqlu"""ALTER TABLE #${MenuView.tableName} ALTER COLUMN #${MenuView.nameColumn} DROP DEFAULT""",
              sqlu"""ALTER TABLE #${MenuView.tableName} ALTER COLUMN #${MenuView.ingredientsColumn} DROP DEFAULT""",
              sqlu"""ALTER TABLE #${MenuView.tableName} ALTER COLUMN #${MenuView.recipeColumn} DROP DEFAULT""",
              sqlu"""ALTER TABLE #${MenuView.tableName} ALTER COLUMN #${MenuView.linkColumn} DROP DEFAULT""",
              sqlu"""ALTER TABLE #${MenuView.tableName} ALTER COLUMN #${MenuView.selectedCountColumn} DROP DEFAULT""",
            )
          )
        case "3.0" =>
          db.run(sqlu"""DROP EXTENSION IF EXISTS "pgcrypto"""")
            .map(_ => ())
      }
    }
  }
}
