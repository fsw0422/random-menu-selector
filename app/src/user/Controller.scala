package src.user

import javax.inject._
import play.api.libs.json.Json
import play.api.mvc.{AbstractController, ControllerComponents}
import scala.concurrent.ExecutionContext

@Singleton
class Controller @Inject()(implicit controllerComponents: ControllerComponents,
                           executionContext: ExecutionContext)
    extends AbstractController(controllerComponents) {

  def createOrUpdateUser() =
    Action.async(parse.json) { implicit request =>
      Aggregate
        .createOrUpdateUser(request.body)
        .map(result => Ok(Json.obj("status" -> Json.toJson(result.toString))))
    }

  def createOrUpdateUserViewSchema() =
    Action.async(parse.json) { implicit request =>
      Aggregate
        .createOrUpdateUserViewSchema(request.body)
        .map(result => Ok(Json.obj("status" -> Json.toJson(result.toString))))
    }
}
