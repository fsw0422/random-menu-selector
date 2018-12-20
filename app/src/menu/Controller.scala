package src.menu

import akka.stream.QueueOfferResult
import javax.inject._
import play.api.libs.json.Json
import play.api.mvc._

import scala.concurrent.ExecutionContext

@Singleton
class Controller @Inject()(implicit controllerComponents: ControllerComponents,
                           executionContext: ExecutionContext)
    extends AbstractController(controllerComponents) {

  def askRandomMenu() = Action.async(parse.formUrlEncoded) { implicit request =>
    Aggregate
      .selectRandomMenu()
      .map {
        case QueueOfferResult.Enqueued =>
          Ok(Json.obj("status" -> Json.toJson("enqueued")))
        case QueueOfferResult.Dropped =>
          Ok(Json.obj("status" -> Json.toJson("dropped")))
      }
  }
}
