package src.user
import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props}
import akka.stream.{
  ActorMaterializer,
  ActorMaterializerSettings,
  OverflowStrategy
}
import akka.stream.scaladsl.{Sink, Source}
import akka.util.Timeout

import scala.concurrent.duration._

object UserAggregate {
  val actorSystem = ActorSystem("UserAggregate")
}

class UserAggregate extends Actor with ActorLogging {
  implicit val executionContext = context.dispatcher
  implicit val actorMaterializer =
    ActorMaterializer(ActorMaterializerSettings(context.system))(context.system)
  implicit val timeout = Timeout(10 seconds)

  var requester: ActorRef = sender()

  override def receive = {
    case cmd: String =>
      cmd match {
        case "getAllUsers" =>
          requester = sender()
          findAllUsers
      }
  }

  private def userSelector = {
    Source
      .queue[String](5, OverflowStrategy.backpressure)
      .ask[List[User]](5)(
        UserAggregate.actorSystem.actorOf(Props[UserRepository])
      )
      .to(Sink.foreach(users => requester ! users))
      .run()
  }

  private def findAllUsers = {
    userSelector offer "findAllUsers"
  }
}
