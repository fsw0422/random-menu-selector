package menu

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, ActorMaterializerSettings}
import com.typesafe.config.Config
import javax.inject.{Inject, Singleton}
import monocle.macros.GenLens
import org.joda.time.DateTime
import play.api.libs.json.{JsValue, Json}
import event.{Event, EventService, EventType}
import user.{UserView, UserViewService}
import utils.{Email, EmailSender}
import scala.util.Random

@Singleton
class Aggregate @Inject()(config: Config,
                          emailSender: EmailSender,
                          eventService: EventService,
                          menuViewService: MenuViewService,
                          userViewService: UserViewService) {

  private implicit val actorSystem = ActorSystem("MenuAggregate")
  private implicit val executionContext = actorSystem.dispatcher
  private implicit val actorMaterializerSettings = ActorMaterializerSettings(
    actorSystem
  )
  private implicit val actorMaterializer = ActorMaterializer(
    actorMaterializerSettings
  )

  def createOrUpdateMenu(menu: JsValue) = {
    val menuView = menu.as[MenuView]
    for {
      updatedMenuView <- menuViewService
        .findByName(menuView.name)
        .map { menuViews =>
          if (menuViews.nonEmpty) {
            val lens = GenLens[MenuView]
            val nameMod = lens(_.name)
              .modify(name => menuViews.head.name)(menuViews.head)
            val ingredientMod = lens(_.ingredients)
              .modify(ingredients => nameMod.ingredients)(nameMod)
            val recipeMod = lens(_.recipe)
              .modify(recipe => ingredientMod.recipe)(ingredientMod)
            lens(_.link).modify(link => recipeMod.link)(recipeMod)
          } else {
            menuView
          }
        }
    } yield {
      val event = Event(
        `type` = EventType.MENU_PROFILE_CREATED_OR_UPDATED,
        data = Some(Json.toJson(updatedMenuView)),
        timestamp = DateTime.now
      )
      eventService.menuEventBus offer event

      updatedMenuView.uuid.get
    }
  }

  def selectRandomMenu() = {
    for {
      menuViews <- menuViewService.findAll()
      userViews <- userViewService.findAll()
      randomMenuView = if (menuViews.nonEmpty) {
        Random.shuffle(menuViews).head
      } else {
        MenuView(
          name = "NONE",
          ingredients = Seq("NONE"),
          recipe = "NONE",
          link = "",
          selectedCount = 0
        )
      }
      updatedRandomMenuView <- menuViewService
        .findByName(randomMenuView.name)
        .map { menuViews =>
          if (menuViews.nonEmpty) {
            GenLens[MenuView](_.selectedCount).modify(_ + 1)(menuViews.head)
          } else {
            randomMenuView
          }
        }
    } yield {
      val event = Event(
        `type` = EventType.RANDOM_MENU_ASKED,
        data = Some(Json.toJson(updatedRandomMenuView)),
        timestamp = DateTime.now
      )
      eventService.menuEventBus offer event

      if (userViews.nonEmpty && menuViews.nonEmpty) {
        sendEmail(randomMenuView, userViews)
      }

      updatedRandomMenuView.uuid.get
    }
  }

  def createOrUpdateMenuViewSchema(version: JsValue) = {
    val event = Event(
      `type` = EventType.MENU_SCHEMA_EVOLVED,
      data = Some(version),
      timestamp = DateTime.now
    )
    eventService.menuEventBus offer event
  }

  private def sendEmail(menu: MenuView, users: Seq[UserView]) = {
    emailSender.send(
      "smtp.gmail.com",
      "465",
      "menuselector0501",
      config.getString("email.password"),
      "menuselector0501@gmail.com",
      "text/html; charset=utf-8",
      Email(
        emails = users.map(user => user.email).toArray,
        subject = "Today's Menu",
        message = s"""
        <html>
          <head>
            <title>Today's Menu</title>
          </head>
          <body>

            <b>Ingredients</b>
            <p>
          ${menu.ingredients
          .foldLeft[String]("")((s, ingredient) => s + "<br>" + ingredient)}
            </p>
            <br>

            <b>Recipe</b>
            <p>
          ${menu.recipe
          .foldLeft[String]("")((s, instruction) => s + "<br>" + instruction)}
            </p>
            <br>

            <b>Link</b>
            <p>
            <a href=${menu.link}>${menu.name}</a>
            </p>

          </body>
        </html>
          """,
      )
    )
  }
}
