package user

import auth.Auth
import javax.inject.{Inject, Singleton}
import play.api.libs.json.Json
import play.api.mvc.{AbstractController, ControllerComponents}
import utils.ResponseMessage

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class CommandController @Inject()(auth: Auth, aggregate: Aggregate)(
  implicit controllerComponents: ControllerComponents,
  executionContext: ExecutionContext
) extends AbstractController(controllerComponents) {

  def createOrUpdateUser() =
    Action.async(parse.json) { implicit request =>
      for {
        isAuth <- auth.checkPassword(request.body)
        result <- {
          if (isAuth) {
            aggregate.createOrUpdateUser(request.body)
          } else {
            Future(ResponseMessage.UNAUTHORIZED)
          }
        }
      } yield Ok(Json.obj("result" -> Json.toJson(result.toString)))
    }

  def createOrUpdateUserViewSchema() =
    Action.async(parse.json) { implicit request =>
      for {
        isAuth <- auth.checkPassword(request.body)
        result <- {
          if (isAuth) {
            aggregate.createOrUpdateUserViewSchema(request.body)
          } else {
            Future(ResponseMessage.UNAUTHORIZED)
          }
        }
      } yield Ok(Json.obj("result" -> Json.toJson(result.toString)))
    }
}
