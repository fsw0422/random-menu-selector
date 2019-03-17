package controllers

import javax.inject.{Inject, Singleton}
import play.api.mvc.{AbstractController, Action, AnyContent, ControllerComponents}

@Singleton
class RedirectController @Inject()
(implicit controllerComponents: ControllerComponents)
  extends AbstractController(controllerComponents) {

  def index(): Action[AnyContent] = Action { implicit request =>
    Redirect("/admin/index.html")
  }
}
