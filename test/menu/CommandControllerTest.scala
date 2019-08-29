package menu

import java.util.{Properties, UUID}

import cats.effect.IO
import com.dimafeng.testcontainers.{ForAllTestContainer, GenericContainer}
import event.EventDao
import org.junit.runner.RunWith
import org.mockito.{ArgumentMatchersSugar, MockitoSugar}
import org.scalatest.junit.JUnitRunner
import org.scalatest.{BeforeAndAfter, FlatSpec, GivenWhenThen, Matchers}
import org.testcontainers.containers.wait.strategy.Wait
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.mvc._
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Helpers}
import user.{User, UserViewDao}
import utils.{Email, EmailSender, ResponseMessage}

@RunWith(classOf[JUnitRunner])
class CommandControllerTest extends FlatSpec
    with ForAllTestContainer
    with MockitoSugar
    with ArgumentMatchersSugar
    with BeforeAndAfter
    with GivenWhenThen
    with Matchers
    with Results {

  private val envMap = sys.props.toMap

  private val emailSenderMock: EmailSender = mock[EmailSender]
  private var mockedApp: Application = _
  private var eventDao: EventDao = _
  private var menuViewDao: MenuViewDao = _
  private var userViewDao: UserViewDao = _

  private val POSTGRES_EXPOSED_PORT = 5432

  override val container = GenericContainer(
    dockerImage = "postgres:9.6",
    exposedPorts = Seq(POSTGRES_EXPOSED_PORT),
    waitStrategy = Wait.forLogMessage(".*database system is ready to accept connections.*\\s", 2),
    env = Map(
      "POSTGRES_PASSWORD" -> envMap("POSTGRES_PASSWORD"),
      "POSTGRES_DB" -> envMap("POSTGRES_DB")
    )
  )

  override def afterStart: Unit = {
    sys.props += (
      "POSTGRES_HOST" -> container.containerIpAddress,
      "POSTGRES_PORT" -> container.mappedPort(POSTGRES_EXPOSED_PORT).toString
    )

    mockedApp = GuiceApplicationBuilder()
      .bindings(bind[EmailSender].toInstance(emailSenderMock))
      .build

    eventDao = mockedApp.injector.instanceOf(classOf[EventDao])
    menuViewDao = mockedApp.injector.instanceOf(classOf[MenuViewDao])
    userViewDao = mockedApp.injector.instanceOf(classOf[UserViewDao])
  }

  override def beforeStop: Unit = {
    mockedApp.stop()
  }

  before {
    eventDao.setup().unsafeRunSync()
    menuViewDao.setup().unsafeRunSync()
    userViewDao.setup().unsafeRunSync()
  }

  after {
    eventDao.teardown().unsafeRunSync()
    menuViewDao.teardown().unsafeRunSync()
    userViewDao.teardown().unsafeRunSync()
  }

  behavior of "POST request to /menu endpoint"

  it should "return ok status" in {
  }

  behavior of "POST request to /menu/random endpoint"

  it should "return ok status with updated " in {
    Given("an apple pie and pear pie")
    val applePieUuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440000")
    val applePieView = Menu(
      Some(applePieUuid),
      "ApplePie",
      Seq("apple", "pie"),
      "bake apple",
      "appleLink"
    )
    menuViewDao.upsert(applePieView).unsafeRunSync()
    val pearPieUuid = UUID.fromString("223e4567-e89b-12d3-a456-426655440000")
    val pearPieView = Menu(
      Some(pearPieUuid),
      "PearPie",
      Seq("pear", "pie"),
      "boil pear",
      "pearLink"
    )
    menuViewDao.upsert(pearPieView).unsafeRunSync()

    And("a default user James")
    val userUuid = UUID.fromString("124e4567-e89b-12d3-a456-426655440000")
    val userView = User(Some(userUuid), "james", "james@email.com")
    userViewDao.upsert(userView).unsafeRunSync()

    // external service contract mock
    And("email is sent to mail server with SMTP protocol if send email is invoked")
    doReturn(IO.pure()).when(emailSenderMock)
      .sendSMTP(any[String], any[String], any[Properties], any[Email])

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
    val result = (responseContent \ "result").as[String]
    result should equal(ResponseMessage.SUCCESS)

    And("menus are sent by email to all users")
    verify(emailSenderMock, times(1))
      .sendSMTP(any[String], any[String], any[Properties], any[Email])
  }
}
