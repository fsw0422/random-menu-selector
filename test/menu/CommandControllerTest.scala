package menu

import java.util.UUID

import event.EventDao
import org.junit.runner.RunWith
import org.mockito.{ArgumentMatchersSugar, MockitoSugar}
import org.scalatest.FunSpec
import org.scalatest.junit.JUnitRunner
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.mvc._
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Helpers}
import user.{UserView, UserViewDao}
import utils.EmailSender

import scala.concurrent.Future

@RunWith(classOf[JUnitRunner])
class CommandControllerTest
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
       GIVEN 2 menus
         AND an user
       WHEN POST request to /menu/random"
  """) {
    it("SHOULD return ok status with either menu's UUID") {
      val applePieUuid = UUID
        .fromString("123e4567-e89b-12d3-a456-426655440000")
      val applePieView = MenuView(
        Some(applePieUuid),
        "ApplePie",
        Seq("apple", "pie"),
        "bake apple",
        "appleLink"
      )

      val pearPieUuid = UUID
        .fromString("223e4567-e89b-12d3-a456-426655440000")
      val pearPieView = MenuView(
        Some(pearPieUuid),
        "PearPie",
        Seq("pear", "pie"),
        "boil pear",
        "pearLink"
      )
      when(
        menuViewDaoMock
          .findAll()
      ).thenReturn(Future(Seq(applePieView, pearPieView)))
      when(menuViewDaoMock.findByName(any[String]))
        .thenReturn(Future(Seq(applePieView)))

      val userUuid = UUID
      .fromString("124e4567-e89b-12d3-a456-426655440000")
      val userView = UserView(Some(userUuid), "james", "james@email.com")
      when(
      userViewDaoMock
        .findAll()
    ).thenReturn(Future(Seq(userView)))
      when(
      userViewDaoMock
        .findByEmail(any[String])
    )
        .thenReturn(Future(Seq(userView)))

      val Some(response) = route(
        mockedApp,
        FakeRequest(Helpers.POST, "/menu/random")
          .withJsonBody(Json.parse("{}"))
      )

      val responseStatus = status(response)
      assert(responseStatus == OK)

      val responseContent = contentAsJson(response)
      val uuid = (responseContent \ "result").as[String]
      assert(
      uuid == "123e4567-e89b-12d3-a456-426655440000" ||
        uuid == "223e4567-e89b-12d3-a456-426655440000"
    )
    }
  }
}
