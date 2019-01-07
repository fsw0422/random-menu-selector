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
import src.{EventDao, EventService}
import src.user.{UserView, UserViewDao, UserViewService}
import src.utils.EmailSender

import scala.concurrent.Future

@RunWith(classOf[JUnitRunner])
class ControllerTest
    extends FunSpec
    with MockitoSugar
    with ArgumentMatchersSugar
    with Results {
  private val emailSenderMock = mock[EmailSender]
  private val eventServiceMock = mock[EventService]
  private val eventDaoMock = mock[EventDao]
  private val menuViewServiceMock = mock[MenuViewService]
  private val menuViewDaoMock = mock[MenuViewDao]
  private val userViewServiceMock = mock[UserViewService]
  private val userViewDaoMock = mock[UserViewDao]

  private val mockedApp = new GuiceApplicationBuilder()
    .bindings(
      bind[EmailSender].toInstance(emailSenderMock),
      //bind[EventService].toInstance(eventServiceMock),
      //bind[EventDao].toInstance(eventDaoMock),
      bind[MenuViewService].toInstance(menuViewServiceMock),
      //bind[MenuViewDao].toInstance(menuViewDaoMock),
      bind[UserViewService].toInstance(userViewServiceMock),
      //bind[UserViewDao].toInstance(userViewDaoMock)
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
      //when(eventServiceMock.storeEvent).thenReturn(Future(Seq(any[MenuView])))

      when(menuViewServiceMock.findByName(any[String]))
        .thenReturn(Future(Seq(any[MenuView])))
      when(menuViewServiceMock.findAll()).thenReturn(Future(Seq(any[MenuView])))

      when(userViewServiceMock.findByEmail(any[String]))
        .thenReturn(Future(Seq(any[UserView])))
      when(userViewServiceMock.findAll()).thenReturn(Future(Seq(any[UserView])))

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
