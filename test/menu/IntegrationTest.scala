package menu

import java.util.UUID

import org.junit.runner.RunWith
import org.mockito.{ArgumentMatchersSugar, MockitoSugar}
import org.scalatest.junit.JUnitRunner
import org.scalatest.{FlatSpec, GivenWhenThen, Matchers}
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder

import scala.concurrent.Await
import scala.concurrent.duration.Duration

@RunWith(classOf[JUnitRunner])
class IntegrationTest extends FlatSpec
  with MockitoSugar
  with ArgumentMatchersSugar
  with GivenWhenThen
  with Matchers {

  private val app: Application = GuiceApplicationBuilder().build
  private val menuViewDao: MenuViewDao = app.injector.instanceOf(classOf[MenuViewDao])

  private val uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001")

  behavior of "findByUuid"
  it should "return the corresponding menu view when searched by uuid" in {
    Given("Empty table in menu view")

    When("Query the menu view by an UUID")
    val result = Await.result(menuViewDao.findByUuid(uuid), Duration.Inf)

    Then("Nothing is returned")
    result should equal(None)
  }
}
