package menu

import java.util.UUID

import javax.inject.{Inject, Singleton}
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{AbstractController, Action, ControllerComponents}

@Singleton
class CommandController @Inject()(aggregate: Aggregate)
  (implicit controllerComponents: ControllerComponents)
  extends AbstractController(controllerComponents) {

  def register(): Action[JsValue] = {
    Action.async(parse.json) { implicit request =>
      val menuOpt = request.body.asOpt[Menu]
      aggregate.register(menuOpt).map {
        case Left(errorMessage: String) =>
          Ok(Json.obj("result" -> Json.toJson(errorMessage)))
        case Right(response: String) =>
          Ok(Json.obj("result" -> Json.toJson(response)))
      }.unsafeToFuture()
    }
  }

  def edit(): Action[JsValue] = {
    Action.async(parse.json) { implicit request =>
      val menuOpt = request.body.asOpt[Menu]
      aggregate.edit(menuOpt).map {
        case Left(errorMessage: String) =>
          Ok(Json.obj("result" -> Json.toJson(errorMessage)))
        case Right(response: String) =>
          Ok(Json.obj("result" -> Json.toJson(response)))
      }.unsafeToFuture()
    }
  }

  def remove(): Action[JsValue] = {
    Action.async(parse.json) { implicit request =>
      val menuOpt = request.body.asOpt[Menu]
      aggregate.remove(menuOpt).map {
        case Left(errorMessage: String) =>
          Ok(Json.obj("result" -> Json.toJson(errorMessage)))
        case Right(response: String) =>
          Ok(Json.obj("result" -> Json.toJson(response)))
      }.unsafeToFuture()
    }
  }

  def selectMenu(): Action[JsValue] = {
    Action.async(parse.json) { implicit request =>
      val uuidOpt = (request.body \ "uuid").asOpt[String].map(UUID.fromString)
      aggregate.selectMenu(uuidOpt).map {
        case Left(errorMessage: String) =>
          Ok(Json.obj("result" -> Json.toJson(errorMessage)))
        case Right(response: String) =>
          Ok(Json.obj("result" -> Json.toJson(response)))
      }.unsafeToFuture()
    }
  }
}
