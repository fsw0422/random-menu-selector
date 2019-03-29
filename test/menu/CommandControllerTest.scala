package menu

import java.util.UUID

import event.EventDao
import org.junit.runner.RunWith
import org.mockito.{ArgumentMatchersSugar, MockitoSugar}
import org.scalatest.junit.JUnitRunner
import org.scalatest.{FlatSpec, GivenWhenThen, Matchers}
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
class CommandControllerTest extends FlatSpec
    with MockitoSugar
    with ArgumentMatchersSugar
    with GivenWhenThen
    with Matchers
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

  behavior of "POST request to /menu/random endpoint"

  it should "return ok status with random menu's UUID" in {
    Given("an apple pie and pear pie")
    val applePieUuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440000")
    val applePieView = MenuView(
      Some(applePieUuid),
      "ApplePie",
      Seq("apple", "pie"),
      "bake apple",
      "appleLink"
    )
    val pearPieUuid = UUID.fromString("223e4567-e89b-12d3-a456-426655440000")
    val pearPieView = MenuView(
      Some(pearPieUuid),
      "PearPie",
      Seq("pear", "pie"),
      "boil pear",
      "pearLink"
    )
    when(menuViewDaoMock.findAll())
      .thenReturn(Future(Seq(applePieView, pearPieView)))
    when(menuViewDaoMock.findByName(any[String]))
      .thenReturn(Future(Seq(applePieView)))

    And("a default user James")
    val userUuid = UUID.fromString("124e4567-e89b-12d3-a456-426655440000")
    val userView = UserView(Some(userUuid), "james", "james@email.com")
    when(userViewDaoMock.findAll())
      .thenReturn(Future(Seq(userView)))
    when(userViewDaoMock.findByEmail(any[String]))
      .thenReturn(Future(Seq(userView)))

    When("request a POST request for random menu")
    val Some(response) = route(
      mockedApp,
      FakeRequest(Helpers.POST, "/menu/random")
        .withJsonBody(Json.parse("{}"))
    )

    Then("return status of ok")
    val responseStatus = status(response)
    responseStatus should equal(OK)

    And("return either apple pie or pear pie")
    val responseContent = contentAsJson(response)
    val uuid = (responseContent \ "result").as[String]
    uuid should (equal("123e4567-e89b-12d3-a456-426655440000") or equal("223e4567-e89b-12d3-a456-426655440000"))

    And("send emails to all users")
    // TODO: check invocation of sendmail
  }
}
