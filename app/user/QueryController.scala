package user

import javax.inject.{Inject, Singleton}
import play.api.libs.json.Json
import play.api.mvc.{AbstractController, Action, AnyContent, ControllerComponents}

import scala.concurrent.ExecutionContext

@Singleton
class QueryController @Inject()(userViewService: UserViewService)(implicit
  controllerComponents: ControllerComponents,
  executionContext: ExecutionContext
) extends AbstractController(controllerComponents) {

  def getAllUsers(): Action[AnyContent] =
    Action.async { implicit request =>
      userViewService.findAll()
        .map { userViews =>
          Ok(Json.obj("result" -> Json.toJson(userViews)))
        }
        .unsafeToFuture()
    }
}
