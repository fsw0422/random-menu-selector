package menu

import javax.inject.{Inject, Singleton}
import play.api.libs.json.Json
import play.api.mvc.{AbstractController, ControllerComponents}

import scala.concurrent.ExecutionContext

@Singleton
class QueryController @Inject()(menuViewService: MenuViewService)(
  implicit controllerComponents: ControllerComponents,
  executionContext: ExecutionContext
) extends AbstractController(controllerComponents) {

  def getAllMenus() =
    Action.async { implicit request =>
      menuViewService
        .findAll()
        .map { menuViews =>
          Ok(Json.obj("result" -> Json.toJson(menuViews)))
        }
    }
}