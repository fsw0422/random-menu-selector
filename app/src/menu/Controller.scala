package src.menu

import javax.inject._
import play.api.libs.json.Json
import play.api.mvc._
import scala.concurrent.ExecutionContext

@Singleton
class Controller @Inject()(implicit controllerComponents: ControllerComponents,
                           executionContext: ExecutionContext)
    extends AbstractController(controllerComponents) {

  def createOrUpdateMenu() =
    Action.async(parse.json) { implicit request =>
      Aggregate
        .createOrUpdateMenu(request.body)
        .map(result => Ok(Json.obj("status" -> Json.toJson(result.toString))))
    }

  def selectRandomMenu() =
    Action.async(parse.json) { implicit request =>
      Aggregate
        .selectRandomMenu()
        .map(result => Ok(Json.obj("status" -> Json.toJson(result.toString))))
    }

  def createOrUpdateMenuViewSchema() =
    Action.async(parse.json) { implicit request =>
      Aggregate
        .createOrUpdateMenuViewSchema(request.body)
        .map(result => Ok(Json.obj("status" -> Json.toJson(result.toString))))
    }
}
