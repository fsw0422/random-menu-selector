package src.menu

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.FunSpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.libs.json.Json
import play.api.mvc._
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Helpers}
import src.EventDao
import src.user.UserViewDao
import src.utils.EmailSender
import org.mockito.MockitoSugar

@RunWith(classOf[JUnitRunner])
class ControllerTest
    extends FunSpec
    with MockitoSugar
    with GuiceOneAppPerSuite
    with Results {
  val emailSender = mock[EmailSender]
  val eventDao = mock[EventDao]
  val menuDao = mock[MenuViewDao]
  val userDao = mock[UserViewDao]

  describe("When POST request to /menu/random") {
      val Some(response) = route(
        app,
        FakeRequest(Helpers.POST, "/menu/random").withJsonBody(Json.parse("{}"))
      )

      it(
        "should return ok status with content indicating that the event has been enqueued"
      ) {
        val responseStatus = status(response)
        assert(responseStatus == OK)

        val responseContent = contentAsJson(response)
        val responseContentStatus = (responseContent \ "status").as[String]
        assert(responseContentStatus == "Enqueued")
      }
  }
}
