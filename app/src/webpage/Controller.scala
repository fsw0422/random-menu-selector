package src.webpage

import javax.inject.Inject
import play.api.mvc.{AbstractController, ControllerComponents}

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class Controller @Inject()()(
  implicit controllerComponents: ControllerComponents,
  executionContext: ExecutionContext
) extends AbstractController(controllerComponents) {

  def createOrUpdateUser() = Action.async(parse.json) { implicit request =>
    Future(OK(src.webpage.index.html))
  }
}
