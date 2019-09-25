package menu

import javax.inject.{Inject, Singleton}
import play.api.libs.json.Json
import play.api.mvc.{AbstractController, Action, AnyContent, ControllerComponents}

@Singleton
class QueryController @Inject()(menuViewHandler: MenuViewHandler)
  (implicit controllerComponents: ControllerComponents)
  extends AbstractController(controllerComponents) {

  def searchByName(name: String): Action[AnyContent] = {
    Action.async { implicit request =>
      menuViewHandler.searchByName(name).map { menuViews =>
        Ok(Json.obj("result" -> Json.toJson(menuViews)))
      }.unsafeToFuture()
    }
  }
}
