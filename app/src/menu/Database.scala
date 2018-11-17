package src.menu
import java.util.UUID

import scala.concurrent.Future

object Database {

  val menus = Future {
    List(
      Menu(
        UUID.fromString("f3fac000-ea5d-11e8-9f32-f2801f1b9fd1"),
        "Asian",
        "Noodle",
        Seq("Green Onion"),
        List("put noodle in hot water", "eat"),
        "http://asian.example.com"
      ),
      Menu(
        UUID.fromString("f3fac28a-ea5d-11e8-9f32-f2801f1b9fd1"),
        "American",
        "Popcorn",
        Seq("corns"),
        List("put in microwave", "put butter", "eat"),
        "http://american.example.com"
      )
    )
  }(MenuRepository.actorSystem.dispatcher)
}
