package controllers

import javax.inject._
import play.api.mvc._

@Singleton
class Controller @Inject()(controllerComponents: ControllerComponents)
    extends AbstractController(controllerComponents) {

  def breakfast() = Action { implicit request =>
    Ok("Breakfast")
  }
}
