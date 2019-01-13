package menu

import javax.inject.{Inject, Singleton}
import play.api.libs.json.Json
import play.api.mvc._
import scala.concurrent.ExecutionContext

@Singleton
class CommandController @Inject()(aggregate: Aggregate)(
  implicit controllerComponents: ControllerComponents,
  executionContext: ExecutionContext
) extends AbstractController(controllerComponents) {

  def createOrUpdateMenu() =
    Action.async(parse.json) { implicit request =>
      aggregate
        .createOrUpdateMenu(request.body)
        .map { result =>
          Ok(Json.obj("uuid" -> Json.toJson(result)))
        }
    }

  def selectRandomMenu() =
    Action.async(parse.json) { implicit request =>
      aggregate
        .selectRandomMenu()
        .map { result =>
          Ok(Json.obj("uuid" -> Json.toJson(result)))
        }
    }

  def createOrUpdateMenuViewSchema() =
    Action.async(parse.json) { implicit request =>
      aggregate
        .createOrUpdateMenuViewSchema(request.body)
        .map { result =>
          Ok(Json.obj("status" -> Json.toJson("Database Evolution Requested")))
        }
    }
}
