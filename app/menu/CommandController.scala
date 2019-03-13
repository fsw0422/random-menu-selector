package menu

import auth.Auth
import javax.inject.{Inject, Singleton}
import play.api.libs.json.{JsValue, Json}
import play.api.mvc._
import utils.ResponseMessage

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class CommandController @Inject()(auth: Auth, aggregate: Aggregate)(
  implicit controllerComponents: ControllerComponents,
  executionContext: ExecutionContext
) extends AbstractController(controllerComponents) {

  def createOrUpdateMenu(): Action[JsValue] =
    Action.async(parse.json) { implicit request =>
      for {
        isAuth <- Future(auth.checkPassword(request.body))
        result <- {
          if (isAuth) {
            Future(aggregate.createOrUpdateMenu(request.body))
          } else {
            Future(ResponseMessage.UNAUTHORIZED)
          }
        }
      } yield Ok(Json.obj("result" -> Json.toJson(result.toString)))
    }

  def deleteMenu(): Action[JsValue] =
    Action.async(parse.json) { implicit request =>
      for {
        isAuth <- Future(auth.checkPassword(request.body))
        result <- {
          if (isAuth) {
            Future(aggregate.deleteMenu(request.body))
          } else {
            Future(ResponseMessage.UNAUTHORIZED)
          }
        }
      } yield Ok(Json.obj("result" -> Json.toJson(result.toString)))
    }

  def selectRandomMenu(): Action[JsValue] =
    Action.async(parse.json) { implicit request =>
      aggregate.selectRandomMenu()
      .map { result =>
        Ok(Json.obj("result" -> Json.toJson(result)))
      }
    }

  def createOrUpdateMenuViewSchema(): Action[JsValue] =
    Action.async(parse.json) { implicit request =>
      for {
        isAuth <- Future(auth.checkPassword(request.body))
        result <- {
          if (isAuth) {
            Future(aggregate.createOrUpdateMenuViewSchema(request.body))
          } else {
            Future(ResponseMessage.UNAUTHORIZED)
          }
        }
      } yield Ok(Json.obj("result" -> Json.toJson(result.toString)))
    }
}
