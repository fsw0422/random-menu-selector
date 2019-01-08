package src.menu

import java.util.UUID

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
import src.event.EventDao
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
  private val menuViewDaoMock = mock[MenuViewDao]
  private val userViewDaoMock = mock[UserViewDao]

  private val mockedApp = new GuiceApplicationBuilder()
    .bindings(
      bind[EmailSender].toInstance(emailSenderMock),
      bind[EventDao].toInstance(eventDaoMock),
      bind[MenuViewDao].toInstance(menuViewDaoMock),
      bind[UserViewDao].toInstance(userViewDaoMock)
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
      val menuView = MenuView(None, "", Seq(""), "", "", 0)
      when(menuViewDaoMock.findAll()).thenReturn(Future(Seq(menuView)))
      when(menuViewDaoMock.findByName(any[String]))
        .thenReturn(Future(Seq(menuView)))

      val userView = UserView(None, "", "")
      when(userViewDaoMock.findAll()).thenReturn(Future(Seq(userView)))
      when(userViewDaoMock.findByEmail(any[String]))
        .thenReturn(Future(Seq(userView)))

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
