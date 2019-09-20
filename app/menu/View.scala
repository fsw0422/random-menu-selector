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
  uuid: UUID,
  name: Option[String],
  ingredients: Option[Seq[String]],
  recipe: Option[String],
  link: Option[String],
  selectedCount: Option[Int]
) {

  def validate[A](notValid: => A)(valid: MenuView => A): A = _ //TODO: implement
}

object MenuView {

  implicit val jsonFormatter = Json.format[MenuView]
}

@Singleton
class ViewHandler @Inject()(
  config: Config,
  emailSender: EmailSender,
  menuViewDao: MenuViewDao,
  userViewDao: UserViewDao
) {

  private val emailUser = config.getString("email.user")
  private val emailPassword = config.getString("email.password")

  def create(menu: Menu): IO[Int] = {
    menu.uuid.fold(IO.pure(0)) { menuUuid =>
      val newMenuView = MenuView(
        uuid = menuUuid,
        name = menu.name,
        ingredients = menu.ingredients,
        recipe = menu.recipe,
        link = menu.link,
        selectedCount = menu.selectedCount
      )
      IO.fromFuture(IO(menuViewDao.upsert(newMenuView)))
    }
  }

  def update(menu: Menu): IO[Int] = {
    menu.uuid.fold(IO.pure(0)) { menuUuid =>
      IO.fromFuture(IO(menuViewDao.findByUuid(menuUuid))).map { menuViews =>
        menuViews.fold(0) { menuView =>
          val newMenuView = menuView.copy(
            uuid = menuUuid,
            name = menu.name.getOrElse(menuView.name),
            ingredients = menu.ingredients.getOrElse(menuView.ingredients),
            recipe = menu.recipe.getOrElse(menuView.recipe),
            link = menu.link.getOrElse(menuView.link),
            selectedCount = menu.selectedCount.getOrElse(menuView.selectedCount)
          )
          IO.fromFuture(IO(menuViewDao.upsert(newMenuView))).unsafeRunSync()
        }
      }
    }
  }

  def delete(uuid: UUID): IO[Int] = {
    IO.fromFuture(IO(menuViewDao.delete(uuid)))
  }

  def sendMenuToAllUsers(menu: Menu): IO[Unit] = {
    menu.uuid.fold(IO.pure(())) { menuUuid =>
      IO.fromFuture(IO(menuViewDao.findByUuid(menuUuid))).flatMap { menuViewOpt =>
        menuViewOpt.fold(IO.pure(())) { menuView =>
          IO.fromFuture(IO(userViewDao.findAll())).flatMap { userViews =>
            sendMenu(menuView, userViews)
          }
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
        recipients = userViews.map(userView => userView.email).toArray,
        subject = "Today's Menu",
        //TODO: make as template
        message =
          s"""
        <html>
          <head>
          </head>
          <body>

            <b>Menu</b>
            <p>
          ${menuView.name}
            </p>
            <br>

            <b>Ingredients</b>
            <p>
          ${menuView.ingredients.mkString("<br>")}
            </p>
            <br>

            <b>Recipe</b>
            <p>
          ${menuView.recipe}
            </p>
            <br>

            <b>Link</b>
            <p>
            <a href=${menuView.link}>${menuView.name}</a>
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
      (uuid, name, ingredients, recipe, link, selectedCount) <> ((MenuView.apply _).tupled, MenuView.unapply)
  }

  private lazy val viewTable = TableQuery[MenuViewTable]

  private lazy val db = DatabaseConfig.forConfig[JdbcProfile]("postgres").db

  def upsert(menuView: MenuView): Future[Int] = db.run {
    viewTable.insertOrUpdate(menuView)
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
