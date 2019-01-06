package src.menu

import org.mockito.{ArgumentMatchersSugar, MockitoSugar}
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.FunSpec
import play.api.libs.json.Json
import play.api.mvc._
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Helpers}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.inject.bind
import src.EventDao
import src.user.{UserView, UserViewDao}
import src.utils.EmailSender

import scala.concurrent.Future

@RunWith(classOf[JUnitRunner])
class ControllerTest
    extends FunSpec
    with MockitoSugar
    with ArgumentMatchersSugar
    with Results {
  private val emailSenderMock = mock[EmailSender]
  private val eventDaoMock = mock[EventDao]
  private val menuDaoMock = mock[MenuViewDao]
  private val userDaoMock = mock[UserViewDao]

  private val mockedApp = new GuiceApplicationBuilder()
    .bindings(
      bind[EmailSender].toInstance(emailSenderMock),
      bind[EventDao].toInstance(eventDaoMock),
      bind[MenuViewDao].toInstance(menuDaoMock),
      bind[UserViewDao].toInstance(userDaoMock)
    )
    .build
  private implicit val dispatcher = mockedApp.actorSystem.dispatcher

  describe("""
       GIVEN a default menu
         AND a default user
       WHEN POST request to /menu/random"
  """) {
    it(
      "SHOULD return ok status with content indicating that the event has been enqueued"
    ) {
      when(menuDaoMock.findAll()).thenReturn(Future(Seq(MenuView())))
      when(userDaoMock.findAll()).thenReturn(Future(Seq(UserView())))

      val Some(response) = route(
        mockedApp,
        FakeRequest(Helpers.POST, "/menu/random").withJsonBody(Json.parse("{}"))
      )

      val responseStatus = status(response)
      assert(responseStatus == OK)

      val responseContent = contentAsJson(response)
      val responseContentStatus = (responseContent \ "status").as[String]
      assert(responseContentStatus == "Enqueued")
    }
  }
}
