package src.user
import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import akka.pattern.{ask, pipe}
import akka.util.Timeout

import scala.concurrent.duration._

object UserAggregate {
  val actorSystem = ActorSystem("UserAggregate")

  val userAggregate = actorSystem.actorOf(Props[UserAggregate])
}

class UserAggregate extends Actor with ActorLogging {
  implicit val executionContext = context.dispatcher
  implicit val timeout = Timeout(10 seconds)

  override def receive = {
    case "getAllUsers" =>
      val users = (UserRepository.userRepository ? "findAllUsers")
        .mapTo[List[User]]
      pipe(users).to(sender())
  }
}
