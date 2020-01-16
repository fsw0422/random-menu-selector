package menu

import java.util.UUID

import cats.effect.IO
import com.typesafe.config.Config
import javax.inject.{Inject, Singleton}
import play.api.libs.json.Json
import slick.basic.DatabaseConfig
import slick.jdbc.JdbcProfile
import user.{UserView, UserViewDao}
import utils.{Email, EmailSender}

import scala.concurrent.Future

final case class MenuView(
  uuid: Option[UUID] = Some(UUID.randomUUID()),
  name: Option[String],
  ingredients: Option[Seq[String]],
  recipe: Option[String],
  link: Option[String],
  selectedCount: Option[Int]
)

object MenuView {

  implicit val jsonFormatter = Json.format[MenuView]
}

@Singleton
class MenuViewHandler @Inject()(
  config: Config,
  emailSender: EmailSender,
  menuViewDao: MenuViewDao,
  userViewDao: UserViewDao
) {

  private val emailUser = config.getString("email.user")
  private val emailPassword = config.getString("email.password")

  def createOrUpdate(menu: Menu): IO[Int] = {
    menu.uuid.fold(IO.pure(0)) { menuUuid =>
      for {
        menuViewOpt <- IO.fromFuture(IO(menuViewDao.findByUuid(menuUuid)))
        newMenuView = menuViewOpt.fold {
          MenuView(
            name = menu.name,
            ingredients = menu.ingredients,
            recipe = menu.recipe,
            link = menu.link,
            selectedCount = Option(0)
          )
        } { menuView =>
          menuView.copy(
            name = menu.name.fold(menuView.name)(name => Some(name)),
            ingredients = menu.ingredients.fold(menuView.ingredients)(ingredients => Some(ingredients)),
            recipe = menu.recipe.fold(menuView.recipe)(recipe => Some(recipe)),
            link = menu.link.fold(menuView.link)(link => Some(link))
          )
        }
        affectedRowNum <- IO.fromFuture(IO(menuViewDao.upsert(newMenuView)))
      } yield affectedRowNum
    }
  }

  def incrementSelectedCount(uuid: UUID): IO[Int] = {
    for {
      menuViewOpt <- IO.fromFuture(IO(menuViewDao.findByUuid(uuid)))
      newMenuViewOpt = menuViewOpt.map { menuView =>
        menuView.copy(selectedCount = menuView.selectedCount.map(_ + 1))
      }
      affectedRowNum <- newMenuViewOpt.fold(IO.pure(0)) { newMenuView =>
        IO.fromFuture(IO(menuViewDao.upsert(newMenuView)))
      }
    } yield affectedRowNum
  }

  def delete(uuid: UUID): IO[Int] = {
    IO.fromFuture(IO(menuViewDao.delete(uuid)))
  }

  def sendMenuToAllUsers(menuUuid: UUID): IO[Unit] = {
    IO.fromFuture(IO(menuViewDao.findByUuid(menuUuid))).flatMap { menuViewOpt =>
      menuViewOpt.fold(IO.pure(())) { menuView =>
        IO.fromFuture(IO(userViewDao.findAll())).flatMap { userViews =>
          sendMenu(menuView, userViews)
        }
      }
    }
  }

  def searchByName(name: String): IO[Seq[MenuView]] = {
    IO.fromFuture(IO(menuViewDao.findByNameLike(name)))
  }

  private def sendMenu(menuView: MenuView, userViews: Seq[UserView]): IO[Unit] = {
    emailSender.sendSMTP(
      emailUser,
      emailPassword,
      Email(
        recipients = userViews.map(userView => userView.email.getOrElse("")).toArray,
        subject = "Today's Menu",
        //TODO: make as template (this is essential for testing)
        message =
          s"""
        <html>
          <head>
          </head>
          <body>

            <b>Menu</b>
            <p>
          ${menuView.name.getOrElse("")}
            </p>
            <br>

            <b>Ingredients</b>
            <p>
          ${menuView.ingredients.getOrElse(List()).mkString("<br>")}
            </p>
            <br>

            <b>Recipe</b>
            <p>
          ${menuView.recipe.getOrElse("")}
            </p>
            <br>

            <b>Link</b>
            <p>
            <a href=${menuView.link.getOrElse("")}>${menuView.name.getOrElse("")}</a>
            </p>

          </body>
        </html>
          """,
      )
    )
  }
}

@Singleton
class MenuViewDao {

  import utils.db.PostgresProfile.api._

  class MenuViewTable(tag: Tag) extends Table[MenuView](tag, "menu_view") {
    def uuid = column[UUID]("uuid", O.PrimaryKey)
    def name = column[String]("name")
    def ingredients = column[Seq[String]]("ingredients")
    def recipe = column[String]("recipe")
    def link = column[String]("link")
    def selectedCount = column[Int]("selected_count")

    def * =
      (uuid.?, name.?, ingredients.?, recipe.?, link.?, selectedCount.?) <> ((MenuView.apply _).tupled, MenuView.unapply)
  }

  private lazy val viewTable = TableQuery[MenuViewTable]

  private lazy val db = DatabaseConfig.forConfig[JdbcProfile]("postgres").db

  def upsert(menuView: MenuView): Future[Int] = db.run {
    menuView.uuid.fold {
      viewTable
        .map(t => (t.name.?, t.ingredients.?, t.recipe.?, t.link.?, t.selectedCount.?))
        .insertOrUpdate((menuView.name, menuView.ingredients, menuView.recipe, menuView.link, menuView.selectedCount))
    } { uuid =>
      viewTable.insertOrUpdate(menuView)
    }
  }

  def findByUuid(uuid: UUID): Future[Option[MenuView]] = db.run {
    viewTable
      .filter(menuView => menuView.uuid === uuid)
      .result
      .headOption
  }

  def findByNameLike(name: String): Future[Seq[MenuView]] = db.run {
    viewTable
      .filter(menuView => menuView.name like "%" + name + "%")
      .result
  }

  def delete(uuid: UUID): Future[Int] = db.run {
    viewTable
      .filter(menuView => menuView.uuid === uuid)
      .delete
  }
}
