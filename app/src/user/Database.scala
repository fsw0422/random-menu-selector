package src.user
import java.util.UUID

import scala.concurrent.Future

object Database {

  val users = Future {
    List(
      User(
        UUID.fromString("f3fac28a-ea5d-11e8-9f32-f2801f1b9fd1"),
        "Kevin Kwon",
        "fsw0422@gmail.com"
      ),
      User(
        UUID.fromString("f3fac28a-ea5d-11e8-9f32-f2801f1b9fd1"),
        "Christina Seifert",
        "xtinaa09@gmail.com"
      )
    )
  }(UserRepository.actorSystem.dispatcher)
}
