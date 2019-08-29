package menu

import javax.inject.{Inject, Singleton}
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{AbstractController, Action, ControllerComponents}

@Singleton
class CommandController @Inject()(aggregate: Aggregate)
  (implicit controllerComponents: ControllerComponents)
  extends AbstractController(controllerComponents) {

  def createOrUpdateMenu(): Action[JsValue] = {
    Action.async(parse.json) { implicit request =>
      aggregate.createOrUpdateMenu(request.body).map {
        case Left(errorMessage: String) =>
          Ok(Json.obj("result" -> Json.toJson(errorMessage)))
        case Right(response: String) =>
          Ok(Json.obj("result" -> Json.toJson(response)))
      }.unsafeToFuture()
    }
  }

  def deleteMenu(): Action[JsValue] = {
    Action.async(parse.json) { implicit request =>
      aggregate.deleteMenu(request.body).map {
        case Left(errorMessage: String) =>
          Ok(Json.obj("result" -> Json.toJson(errorMessage)))
        case Right(response: String) =>
          Ok(Json.obj("result" -> Json.toJson(response)))
      }.unsafeToFuture()
    }
  }

  def selectRandomMenu(): Action[JsValue] = {
    Action.async(parse.json) { implicit request =>
      aggregate.selectMenuRandom().map {
        case Left(errorMessage: String) =>
          Ok(Json.obj("result" -> Json.toJson(errorMessage)))
        case Right(response: String) =>
          Ok(Json.obj("result" -> Json.toJson(response)))
      }.unsafeToFuture()
    }
  }
}
