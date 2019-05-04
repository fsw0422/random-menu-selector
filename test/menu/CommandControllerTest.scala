package menu

import java.util.UUID

import com.dimafeng.testcontainers.{FixedHostPortGenericContainer, ForAllTestContainer}
import event.EventDao
import org.junit.runner.RunWith
import org.mockito.{ArgumentMatchersSugar, MockitoSugar}
import org.scalatest.junit.JUnitRunner
import org.scalatest.{BeforeAndAfter, FlatSpec, GivenWhenThen, Matchers}
import org.testcontainers.containers.wait.strategy.Wait
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.mvc._
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Helpers}
import user.{UserView, UserViewDao}
import utils.{EmailSender, EmailSenderMock}

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

  override val container = FixedHostPortGenericContainer(
    "postgres:9.6",
    waitStrategy = Wait.forLogMessage(".*database system is ready to accept connections.*\\s", 2),
    exposedHostPort = envMap("POSTGRES_PORT").toInt,
    exposedContainerPort = 5432,
    env = Map(
      "POSTGRES_PASSWORD" -> envMap("POSTGRES_PASSWORD"),
      "POSTGRES_DB" -> envMap("POSTGRES_DB")
    )
  )

  private val emailSenderMock = new EmailSenderMock
  private val mockedApp = new GuiceApplicationBuilder()
    .bindings(bind[EmailSender].toInstance(emailSenderMock))
    .build

  private val eventDao = mockedApp.injector.instanceOf(classOf[EventDao])
  private val menuViewDao = mockedApp.injector.instanceOf(classOf[MenuViewDao])
  private val userViewDao = mockedApp.injector.instanceOf(classOf[UserViewDao])

  private implicit val dispatcher = mockedApp.actorSystem.dispatcher

  before {
    eventDao.setup().unsafeRunSync()

    menuViewDao.setup().unsafeRunSync()
    menuViewDao.evolve("1.0").unsafeRunSync()
    menuViewDao.evolve("2.0").unsafeRunSync()
    menuViewDao.evolve("3.0").unsafeRunSync()

    userViewDao.setup().unsafeRunSync()
    userViewDao.evolve("1.0").unsafeRunSync()
    userViewDao.evolve("2.0").unsafeRunSync()
    userViewDao.evolve("3.0").unsafeRunSync()
  }

  after {
    eventDao.teardown().unsafeRunSync()
    menuViewDao.teardown().unsafeRunSync()
    userViewDao.teardown().unsafeRunSync()
  }

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
    menuViewDao.upsert(applePieView).unsafeRunSync()
    val pearPieUuid = UUID.fromString("223e4567-e89b-12d3-a456-426655440000")
    val pearPieView = MenuView(
      Some(pearPieUuid),
      "PearPie",
      Seq("pear", "pie"),
      "boil pear",
      "pearLink"
    )
    menuViewDao.upsert(pearPieView).unsafeRunSync()

    And("a default user James")
    val userUuid = UUID.fromString("124e4567-e89b-12d3-a456-426655440000")
    val userView = UserView(Some(userUuid), "james", "james@email.com")
    userViewDao.upsert(userView).unsafeRunSync()

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
    //TODO: check with real gmail account
  }
}
