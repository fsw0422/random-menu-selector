package user

import javax.inject.{Inject, Singleton}
import play.api.libs.json.Json
import play.api.mvc.{AbstractController, ControllerComponents}
import scala.concurrent.ExecutionContext

@Singleton
class CommandController @Inject()(aggregate: Aggregate)(
  implicit controllerComponents: ControllerComponents,
  executionContext: ExecutionContext
) extends AbstractController(controllerComponents) {

  def createOrUpdateUser() =
    Action.async(parse.json) { implicit request =>
      aggregate
        .createOrUpdateUser(request.body)
        .map { result =>
          Ok(Json.obj("uuid" -> Json.toJson(result)))
        }
    }

  def createOrUpdateUserViewSchema() =
    Action.async(parse.json) { implicit request =>
      aggregate
        .createOrUpdateUserViewSchema(request.body)
        .map { result =>
          Ok(Json.obj("status" -> Json.toJson("Database Evolution Requested")))
        }
    }
}
