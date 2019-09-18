package menu

import java.util.UUID

import cats.effect.IO
import org.junit.runner.RunWith
import org.mockito.{ArgumentMatchersSugar, MockitoSugar}
import org.scalatest.junit.JUnitRunner
import org.scalatest.{FlatSpec, GivenWhenThen, Matchers}
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.mvc._
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Helpers}
import utils.ResponseMessage

/*
 * Testing only HTTP stack values such as return code, passing cookie exchange, header pass-through, etc.
 */
@RunWith(classOf[JUnitRunner])
class CommandControllerTest extends FlatSpec
    with MockitoSugar
    with ArgumentMatchersSugar
    with GivenWhenThen
    with Matchers
    with Results {

  private val aggregateMock: Aggregate = mock[Aggregate]

  private val mockedApp: Application = GuiceApplicationBuilder()
    .overrides(bind[Aggregate].to(aggregateMock))
    .build

  behavior of "POST request to /v1/menu endpoint"
  it should "return \"OK\" status with \"" + ResponseMessage.SUCCESS + "\" message" in {
    Given("Register returns Success Response message")
    doReturn(IO.pure(Right(ResponseMessage.SUCCESS))).when(aggregateMock).register(any[Option[Menu]])

    When("Request menu register")
    val Some(response) = route(
      mockedApp,
      FakeRequest(Helpers.POST, "/v1/menu")
        .withJsonBody(Json.parse("{}"))
    )

    Then("Return status of \"OK\"")
    val responseStatus = status(response)
    responseStatus should equal(OK)
    And("\"SUCCESS\" message")
    val responseContent = contentAsJson(response)
    val result = (responseContent \ "result").as[String]
    result should equal(ResponseMessage.SUCCESS)
  }

  behavior of "PUT request to /v1/menu endpoint"
  it should "return \"OK\" status with \"" + ResponseMessage.SUCCESS + "\" message" in {
    Given("Edit returns Success Response message")
    doReturn(IO.pure(Right(ResponseMessage.SUCCESS))).when(aggregateMock).edit(any[Option[Menu]])

    When("Request menu register")
    val Some(response) = route(
      mockedApp,
      FakeRequest(Helpers.PUT, "/v1/menu")
        .withJsonBody(Json.parse("{}"))
    )

    Then("Return status of \"OK\"")
    val responseStatus = status(response)
    responseStatus should equal(OK)
    And("\"SUCCESS\" message")
    val responseContent = contentAsJson(response)
    val result = (responseContent \ "result").as[String]
    result should equal(ResponseMessage.SUCCESS)
  }

  behavior of "DELETE request to /v1/menu endpoint"
  it should "return \"OK\" status with \"" + ResponseMessage.SUCCESS + "\" message" in {
    Given("Remove returns Success Response message")
    doReturn(IO.pure(Right(ResponseMessage.SUCCESS))).when(aggregateMock).remove(any[Option[Menu]])

    When("Request menu register")
    val Some(response) = route(
      mockedApp,
      FakeRequest(Helpers.DELETE, "/v1/menu")
        .withJsonBody(Json.parse("{}"))
    )

    Then("Return status of \"OK\"")
    val responseStatus = status(response)
    responseStatus should equal(OK)
    And("\"SUCCESS\" message")
    val responseContent = contentAsJson(response)
    val result = (responseContent \ "result").as[String]
    result should equal(ResponseMessage.SUCCESS)
  }
}
