package user

import java.util.UUID

import auth.Auth
import javax.inject.{Inject, Singleton}
import play.api.Configuration
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{AbstractController, Action, ControllerComponents}

import scala.concurrent.ExecutionContext

@Singleton
class CommandController @Inject()(
  auth: Auth,
  aggregate: Aggregate
)(implicit
  controllerComponents: ControllerComponents,
  configuration: Configuration,
  executionContext: ExecutionContext
) extends AbstractController(controllerComponents) {

  def createOrUpdateUser(): Action[JsValue] =
    Action.async(parse.json) { implicit request =>
      aggregate.createOrUpdateUser(request.body)
        .map {
          case Left(errorMessage: String) =>
            Ok(Json.obj("result" -> Json.toJson(errorMessage)))
          case Right(uuidOpt: Option[UUID]) =>
            Ok(Json.obj("result" -> Json.toJson(uuidOpt)))
        }
        .unsafeToFuture()
    }

  def deleteUser(): Action[JsValue] =
    Action.async(parse.json) { implicit request =>
      aggregate.deleteUser(request.body)
        .map {
          case Left(errorMessage: String) =>
            Ok(Json.obj("result" -> Json.toJson(errorMessage)))
          case Right(uuidOpt: Option[UUID]) =>
            Ok(Json.obj("result" -> Json.toJson(uuidOpt)))
        }
        .unsafeToFuture()
    }

  def createOrUpdateUserViewSchema(): Action[JsValue] =
    Action.async(parse.json) { implicit request =>
      aggregate.createOrUpdateUserViewSchema(request.body)
        .map {
          case Left(message: String) =>
            Ok(Json.obj("result" -> Json.toJson(message)))
          case Right(_) =>
            Ok
        }
        .unsafeToFuture()
    }
}
