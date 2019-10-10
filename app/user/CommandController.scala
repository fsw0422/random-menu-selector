package user

import javax.inject.{Inject, Singleton}
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{AbstractController, Action, ControllerComponents}

@Singleton
class CommandController @Inject()(aggregate: Aggregate)
  (implicit controllerComponents: ControllerComponents)
  extends AbstractController(controllerComponents) {

  def signUp(): Action[JsValue] = {
    Action.async(parse.json) { implicit request =>
      val userOpt = request.body.asOpt[User]
      aggregate.signUp(userOpt).map {
        case Left(errorMessage: String) =>
          Ok(Json.obj("result" -> Json.toJson(errorMessage)))
        case Right(response: String) =>
          Ok(Json.obj("result" -> Json.toJson(response)))
      }.unsafeToFuture()
    }
  }

  def edit(): Action[JsValue] = {
    Action.async(parse.json) { implicit request =>
      val userOpt = request.body.asOpt[User]
      aggregate.edit(userOpt).map {
        case Left(errorMessage: String) =>
          Ok(Json.obj("result" -> Json.toJson(errorMessage)))
        case Right(response: String) =>
          Ok(Json.obj("result" -> Json.toJson(response)))
      }.unsafeToFuture()
    }
  }

  def remove(): Action[JsValue] = {
    Action.async(parse.json) { implicit request =>
      val userOpt = request.body.asOpt[User]
      aggregate.remove(userOpt).map {
        case Left(errorMessage: String) =>
          Ok(Json.obj("result" -> Json.toJson(errorMessage)))
        case Right(response: String) =>
          Ok(Json.obj("result" -> Json.toJson(response)))
      }.unsafeToFuture()
    }
  }
}
