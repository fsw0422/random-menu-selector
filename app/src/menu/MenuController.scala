package src.menu

import akka.pattern.ask
import akka.util.Timeout
import javax.inject._
import play.api.libs.json._
import play.api.mvc._
import scala.concurrent.duration._

@Singleton
class MenuController @Inject()(
  implicit controllerComponents: ControllerComponents
) extends AbstractController(controllerComponents) {

  implicit val jsonFormat = Json.format[Menu]
  implicit val timeout = Timeout(10 seconds)

  def menu() = Action.async { implicit request =>
    (MenuAggregate.selector ? "selectByRequest")
      .mapTo[Menu]
      .map { result =>
        Ok(
          Json
            .obj("data" -> Json.toJson(result))
        )
      }(MenuAggregate.actorSystem.dispatcher)
  }
}
