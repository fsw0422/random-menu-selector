package src.menu

import akka.actor.{ActorSystem, Props}
import akka.stream.Materializer
import akka.stream.scaladsl.Source
import akka.util.Timeout
import javax.inject._
import play.api.libs.json._
import play.api.mvc._

import scala.concurrent.ExecutionContextExecutor
import scala.concurrent.duration._

@Singleton
class MenuController @Inject()(
  implicit controllerComponents: ControllerComponents,
  actorSystem: ActorSystem,
  executionContextExecutor: ExecutionContextExecutor,
  materializer: Materializer
) extends AbstractController(controllerComponents) {

  implicit val timeout = Timeout(5 seconds)
  implicit val jsonFormat = Json.format[Menu]

  val menuAggregate = actorSystem.actorOf(Props[MenuAggregate])

  def menu(typ: String) = Action.async { implicit request =>
    Source(List(typ))
      .ask[Menu](parallelism = 5)(menuAggregate)
      .runReduce((m1, m2) => m1)
      .map(result => Ok(Json.obj("data" -> Json.toJson(result))))
  }
}
