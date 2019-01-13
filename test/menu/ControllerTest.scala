package menu

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
import event.EventDao
import user.{UserView, UserViewDao}
import utils.EmailSender

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
      val menuUuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440000")
      val menuView = MenuView(Some(menuUuid), "", Seq(""), "", "", 0)
      when(menuViewDaoMock.findAll()).thenReturn(Future(Seq(menuView)))
      when(menuViewDaoMock.findByName(any[String]))
        .thenReturn(Future(Seq(menuView)))

      val userUuid = UUID.fromString("223e4567-e89b-12d3-a456-426655440000")
      val userView = UserView(Some(userUuid), "", "")
      when(userViewDaoMock.findAll()).thenReturn(Future(Seq(userView)))
      when(userViewDaoMock.findByEmail(any[String]))
        .thenReturn(Future(Seq(userView)))

      val Some(response) = route(
        mockedApp,
        FakeRequest(Helpers.POST, "/menu/random")
          .withJsonBody(Json.parse("{}"))
      )

      val responseStatus = status(response)
      assert(responseStatus == OK)

      val responseContent = contentAsJson(response)
      val uuid = (responseContent \ "uuid").as[String]
      assert(uuid == "123e4567-e89b-12d3-a456-426655440000")
    }
  }
}
