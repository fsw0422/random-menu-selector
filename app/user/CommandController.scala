package user

import java.util.UUID

import javax.inject.{Inject, Singleton}
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{AbstractController, Action, ControllerComponents}

@Singleton
class CommandController @Inject()(aggregate: Aggregate)
  (implicit controllerComponents: ControllerComponents)
  extends AbstractController(controllerComponents) {

  def createOrUpdateUser(): Action[JsValue] = {
    Action.async(parse.json) { implicit request =>
      aggregate.createOrUpdateUser(request.body).map {
        case Left(errorMessage: String) =>
          Ok(Json.obj("result" -> Json.toJson(errorMessage)))
        case Right(uuidOpt: Option[UUID]) =>
          Ok(Json.obj("result" -> Json.toJson(uuidOpt)))
      }.unsafeToFuture()
    }
  }

  def deleteUser(): Action[JsValue] = {
    Action.async(parse.json) { implicit request =>
      aggregate.deleteUser(request.body).map {
        case Left(errorMessage: String) =>
          Ok(Json.obj("result" -> Json.toJson(errorMessage)))
        case Right(uuidOpt: Option[UUID]) =>
          Ok(Json.obj("result" -> Json.toJson(uuidOpt)))
      }.unsafeToFuture()
    }
  }
}
