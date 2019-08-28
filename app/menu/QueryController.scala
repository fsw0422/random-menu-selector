package menu

import javax.inject.{Inject, Singleton}
import play.api.libs.json.Json
import play.api.mvc.{AbstractController, Action, AnyContent, ControllerComponents}

@Singleton
class QueryController @Inject()(menuViewDao: MenuViewDao)
  (implicit controllerComponents: ControllerComponents)
  extends AbstractController(controllerComponents) {

  def getMenusByNameLike(name: String): Action[AnyContent] = {
    Action.async { implicit request =>
      menuViewDao.findByNameLike(name).map { menuViews =>
        Ok(Json.obj("result" -> Json.toJson(menuViews)))
      }.unsafeToFuture()
    }
  }
}
