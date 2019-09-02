import com.dimafeng.testcontainers.{ForAllTestContainer, GenericContainer}
import event.EventDao
import menu.MenuViewDao
import org.junit.runner.RunWith
import org.scalatest.FlatSpec
import org.scalatest.junit.JUnitRunner
import org.testcontainers.containers.wait.strategy.Wait
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import user.UserViewDao

/*
 * Testing the integration of the infrastructure resources that the application owns.
 * It will test the integration of real database, queues, etc.
 * This test in general will not contain any tests but running of the application.
 * If there are any custom SQL / interaction logic with the infra resource with app, test it here.
 * This test will not test third party service integration.
 * Third party app coverage will only be covered with contract test (PACT etc.) and smoke test in CD
 * Also due to eager binding, this will test if all DI container is properly wired as well
 */
@RunWith(classOf[JUnitRunner])
class IntegrationTest extends FlatSpec
  with ForAllTestContainer {

  private val envMap = sys.props.toMap

  private val POSTGRES_EXPOSED_PORT = 5432

  private var eventDao: EventDao = _
  private var menuViewDao: MenuViewDao = _
  private var userViewDao: UserViewDao = _

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
    // replace env variable with container generated host / port
    // this is to avoid container clash with the same host / port while testing in parallel in CI environment
    sys.props += (
      "POSTGRES_HOST" -> container.containerIpAddress,
      "POSTGRES_PORT" -> container.mappedPort(POSTGRES_EXPOSED_PORT).toString
    )

    val mockedApp: Application = GuiceApplicationBuilder()
      // eagerly load all components, as the database will not be configured unless there are real transactions
      .eagerlyLoaded()
      .build

    // for testing custom SQL (in case there are any custom SQL written
    eventDao = mockedApp.injector.instanceOf(classOf[EventDao])
    menuViewDao = mockedApp.injector.instanceOf(classOf[MenuViewDao])
    userViewDao = mockedApp.injector.instanceOf(classOf[UserViewDao])
  }
}
