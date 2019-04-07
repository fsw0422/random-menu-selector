package menu

import java.util.UUID

import javax.inject.{Inject, Singleton}
import play.api.libs.json.{JsValue, Json}
import play.api.mvc._

import scala.concurrent.ExecutionContext

@Singleton
class CommandController @Inject()(aggregate: Aggregate)(implicit
  controllerComponents: ControllerComponents,
  executionContext: ExecutionContext
) extends AbstractController(controllerComponents) {

  def createOrUpdateMenu(): Action[JsValue] =
    Action.async(parse.json) { implicit request =>
      aggregate.createOrUpdateMenu(request.body)
        .map {
          case Left(errorMessage: String) =>
            Ok(Json.obj("result" -> Json.toJson(errorMessage)))
          case Right(uuidOpt: Option[UUID]) =>
            Ok(Json.obj("result" -> Json.toJson(uuidOpt)))
        }
        .unsafeToFuture()
    }

  def deleteMenu(): Action[JsValue] =
    Action.async(parse.json) { implicit request =>
      aggregate.deleteMenu(request.body)
        .map {
          case Left(errorMessage: String) =>
            Ok(Json.obj("result" -> Json.toJson(errorMessage)))
          case Right(uuidOpt: Option[UUID]) =>
            Ok(Json.obj("result" -> Json.toJson(uuidOpt)))
        }
        .unsafeToFuture()
    }

  def selectRandomMenu(): Action[JsValue] =
    Action.async(parse.json) { implicit request =>
      aggregate.selectRandomMenu()
        .map {
          case Left(errorMessage: String) =>
            Ok(Json.obj("result" -> Json.toJson(errorMessage)))
          case Right(uuidOpt: Option[UUID]) =>
            Ok(Json.obj("result" -> Json.toJson(uuidOpt)))
        }
        .unsafeToFuture()
    }

  def createOrUpdateMenuViewSchema(): Action[JsValue] =
    Action.async(parse.json) { implicit request =>
      aggregate.createOrUpdateMenuViewSchema(request.body)
        .map {
          case Left(message: String) =>
            Ok(Json.obj("result" -> Json.toJson(message)))
          case Right(_) =>
            Ok
        }
        .unsafeToFuture()
    }
}
