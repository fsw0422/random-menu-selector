package src.user

import javax.inject._
import play.api.libs.json.Json
import play.api.mvc._

import scala.concurrent.ExecutionContext

@Singleton
class Controller @Inject()(implicit controllerComponents: ControllerComponents,
                           executionContext: ExecutionContext)
    extends AbstractController(controllerComponents) {}

object JsonFormatter {
  implicit val jsonFormat = Json.format[UserView]
}
