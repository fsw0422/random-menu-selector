package menu

import javax.inject.{Inject, Singleton}
import play.api.libs.json.Json
import play.api.mvc.{AbstractController, Action, AnyContent, ControllerComponents}

import scala.concurrent.ExecutionContext

@Singleton
class QueryController @Inject()(menuViewService: MenuViewService)(implicit
  controllerComponents: ControllerComponents,
  executionContext: ExecutionContext
) extends AbstractController(controllerComponents) {

  def getMenusByNameLike(name: String): Action[AnyContent] = {
    Action.async { implicit request =>
      menuViewService.findByNameLike(name).map { menuViews =>
        Ok(Json.obj("result" -> Json.toJson(menuViews)))
      }.unsafeToFuture()
    }
  }
}
