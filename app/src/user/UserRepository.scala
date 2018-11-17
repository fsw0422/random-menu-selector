package src.user
import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import akka.pattern.pipe

object UserRepository {
  val actorSystem = ActorSystem("UserRepository")

  val userRepository = actorSystem.actorOf(Props[UserRepository])
}

class UserRepository extends Actor with ActorLogging {
  implicit val executionContext = context.dispatcher

  override def receive = {
    case "findAllUsers" =>
      pipe(Database.users).to(sender())
  }
}
