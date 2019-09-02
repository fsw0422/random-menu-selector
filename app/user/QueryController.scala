package user

import javax.inject.{Inject, Singleton}
import play.api.libs.json.Json
import play.api.mvc.{AbstractController, Action, AnyContent, ControllerComponents}

@Singleton
class QueryController @Inject()(userViewHandler: UserViewHandler)
  (implicit controllerComponents: ControllerComponents)
  extends AbstractController(controllerComponents) {

  def searchAll(): Action[AnyContent] = {
    Action.async { implicit request =>
      userViewHandler.searchAll().map { userViews =>
        Ok(Json.obj("result" -> Json.toJson(userViews)))
      }.unsafeToFuture()
    }
  }
}
