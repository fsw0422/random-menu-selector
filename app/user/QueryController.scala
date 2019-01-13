package user

import javax.inject.{Inject, Singleton}
import play.api.libs.json.Json
import play.api.mvc.{AbstractController, ControllerComponents}

import scala.concurrent.ExecutionContext

@Singleton
class QueryController @Inject()(userViewService: UserViewService)(
  implicit controllerComponents: ControllerComponents,
  executionContext: ExecutionContext
) extends AbstractController(controllerComponents) {

  def getAllMenus() =
    Action.async(parse.json) { implicit request =>
      userViewService.findAll().map { userViews =>
        Ok(Json.obj("result" -> Json.toJson(userViews)))
      }
    }
}
