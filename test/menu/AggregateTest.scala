package menu

import java.util.{Properties, UUID}

import com.typesafe.config.{Config, ConfigFactory}
import event.EventType.EventType
import event.{Event, EventDao, EventHandler, EventType}
import javax.mail.internet.InternetAddress
import javax.mail.{Address, Message, Session, Transport}
import org.joda.time.DateTime
import org.junit.runner.RunWith
import org.mockito.{ArgumentCaptor, ArgumentMatchersSugar, MockitoSugar}
import org.scalatest.junit.JUnitRunner
import org.scalatest.{BeforeAndAfterEach, FlatSpec, GivenWhenThen, Matchers}
import user.{UserView, UserViewDao}
import utils.{EmailSender, ResponseMessage}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@RunWith(classOf[JUnitRunner])
class AggregateTest extends FlatSpec
  with MockitoSugar
  with BeforeAndAfterEach
  with ArgumentMatchersSugar
  with GivenWhenThen
  with Matchers {

  private var eventDao: EventDao = _
  private var menuViewDao: MenuViewDao = _
  private var userViewDao: UserViewDao = _
  private var transport: Transport = _
  private var aggregate: Aggregate = _

  private val uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001")

  override def beforeEach: Unit = {
    eventDao = mock[EventDao]
    menuViewDao = mock[MenuViewDao]
    userViewDao = mock[UserViewDao]

    val config: Config = ConfigFactory.load("application.conf")

    transport = mock[Transport]
    val session: Session = Session.getInstance(new Properties())
    val emailSender: EmailSender = new EmailSender(session, transport)
    val eventHandler: EventHandler = new EventHandler(eventDao)
    val menuViewHandler: MenuViewHandler = new MenuViewHandler(config, emailSender, menuViewDao, userViewDao)

    aggregate = new Aggregate(config, eventHandler, menuViewHandler)
  }

  behavior of "register"
  it should "register a new menu" in {
    Given("An event insertion is successful")
    doReturn(Future(1)).when(eventDao).insert(any[Event])
    And("menu insertion is successful")
    doReturn(Future(1)).when(menuViewDao).upsert(any[MenuView])
    And("menu does not exist")
    doReturn(Future(None)).when(menuViewDao).findByUuid(any[UUID])

    When("Register menu")
    val menu = Menu(
      uuid = None,
      name = Some("Rice Crispy"),
      ingredients = Some(Seq("ketchup", "mayo")),
      recipe = Some("blahblahblah"),
      link = Some("http://haha.com"),
      passwordAttempt = Some("fake")
    )
    val result = aggregate.register(Some(menu)).unsafeRunSync()

    Then("Result of registration return a SUCCESS message")
    result should equal(Right(ResponseMessage.SUCCESS))
    And("event insert is called once")
    val eventCaptor: ArgumentCaptor[Event] = ArgumentCaptor.forClass(classOf[Event])
    verify(eventDao, times(1)).insert(eventCaptor.capture())
    assertMenuEvent(eventCaptor.getValue, EventType.MENU_CREATED, menu)
    And("menu insert is called once")
    val menuViewCaptor: ArgumentCaptor[MenuView] = ArgumentCaptor.forClass(classOf[MenuView])
    verify(menuViewDao, times(1)).upsert(menuViewCaptor.capture())
    assertMenuView(menuViewCaptor.getValue, menu)
  }

  behavior of "edit"
  it should "edit an existing menu" in {
    Given("An event insertion is successful")
    doReturn(Future(1)).when(eventDao).insert(any[Event])
    And("menu update is successful")
    doReturn(Future(1)).when(menuViewDao).upsert(any[MenuView])
    And("menu exists")
    val menuView = MenuView(
      uuid = Some(uuid),
      name = Some("Rice Crispy"),
      ingredients = Some(Seq("ketchup", "mayo")),
      recipe = Some("blahblahblah"),
      link = Some("http://haha.com"),
      selectedCount = Some(0)
    )
    doReturn(Future(Option(menuView))).when(menuViewDao).findByUuid(any[UUID])

    When("Edit menu")
    val menu = Menu(
      uuid = Some(uuid),
      name = Some("Rice Dipspy"),
      ingredients = Some(Seq("fart", "toenail")),
      recipe = Some("dunno"),
      link = Some("http://heha.com"),
      passwordAttempt = Some("fake")
    )
    val result = aggregate.edit(Some(menu)).unsafeRunSync()

    Then("Result of registration return a SUCCESS message")
    result should equal(Right(ResponseMessage.SUCCESS))
    And("event insert is called once")
    val eventCaptor: ArgumentCaptor[Event] = ArgumentCaptor.forClass(classOf[Event])
    verify(eventDao, times(1)).insert(eventCaptor.capture())
    assertMenuEvent(eventCaptor.getValue, EventType.MENU_UPDATED, menu)
    And("menu update is called once")
    val menuViewCaptor: ArgumentCaptor[MenuView] = ArgumentCaptor.forClass(classOf[MenuView])
    verify(menuViewDao, times(1)).upsert(menuViewCaptor.capture())
    assertMenuView(menuViewCaptor.getValue, menu)
  }

  behavior of "remove"
  it should "remove an existing menu" in {
    Given("An event insertion is successful")
    doReturn(Future(1)).when(eventDao).insert(any[Event])
    And("menu deletion is successful")
    doReturn(Future(1)).when(menuViewDao).delete(any[UUID])

    When("Remove menu")
    val menu = Menu(
      uuid = Some(uuid),
      name = None,
      ingredients = None,
      recipe = None,
      link = None,
      passwordAttempt = Some("fake")
    )
    val result = aggregate.remove(Some(menu)).unsafeRunSync()

    Then("Result of registration return a SUCCESS message")
    result should equal(Right(ResponseMessage.SUCCESS))
    And("event insert is called once")
    val eventCaptor: ArgumentCaptor[Event] = ArgumentCaptor.forClass(classOf[Event])
    verify(eventDao, times(1)).insert(eventCaptor.capture())
    val eventArg = eventCaptor.getValue
    eventArg.`type`.get should equal(EventType.MENU_DELETED)
    eventArg.aggregate.get should equal(Menu.aggregateName)
    And("menu delete is called once")
    val uuidCaptor: ArgumentCaptor[UUID] = ArgumentCaptor.forClass(classOf[UUID])
    verify(menuViewDao, times(1)).delete(uuidCaptor.capture())
    uuidCaptor.getValue should equal(menu.uuid.get)
  }

  behavior of "selectMenu"
  it should "select existing menu" in {
    Given("An event insertion is successful")
    doReturn(Future(1)).when(eventDao).insert(any[Event])
    And("menu update is successful")
    doReturn(Future(1)).when(menuViewDao).upsert(any[MenuView])
    And("menu to edit already exists")
    val menuView = MenuView(
      uuid = Some(uuid),
      name = Some("Rice Crispy"),
      ingredients = Some(Seq("ketchup", "mayo")),
      recipe = Some("blahblahblah"),
      link = Some("http://haha.com"),
      selectedCount = Some(0)
    )
    doReturn(Future(Option(menuView))).when(menuViewDao).findByUuid(any[UUID])
    And("users to send already exists")
    val userView = UserView(
      name = Some("me"),
      email = Some("asdf@me.com")
    )
    val userViews = Seq(userView)
    doReturn(Future(userViews)).when(userViewDao).findAll()

    When("Select menu")
    val result = aggregate.selectMenu(Some(uuid)).unsafeRunSync()

    Then("Result of registration return a SUCCESS message")
    result should equal(Right(ResponseMessage.SUCCESS))
    And("event insert is called once")
    val eventCaptor: ArgumentCaptor[Event] = ArgumentCaptor.forClass(classOf[Event])
    verify(eventDao, times(1)).insert(eventCaptor.capture())
    val eventArg = eventCaptor.getValue
    eventArg.`type`.get should equal(EventType.MENU_SELECTED)
    eventArg.aggregate.get should equal(Menu.aggregateName)
    And("menu update is called once")
    val menuViewCaptor: ArgumentCaptor[MenuView] = ArgumentCaptor.forClass(classOf[MenuView])
    verify(menuViewDao, times(1)).upsert(menuViewCaptor.capture())
    val menuViewArg = menuViewCaptor.getValue
    And("send email is called once")
    val messageCaptor: ArgumentCaptor[Message] = ArgumentCaptor.forClass(classOf[Message])
    val addressCaptor: ArgumentCaptor[Array[Address]] = ArgumentCaptor.forClass(classOf[Array[Address]])
    verify(transport, times(1)).sendMessage(messageCaptor.capture(), addressCaptor.capture())
    //TODO: validate message content
    //val messageArg = messageCaptor.getValue
    val addressArg = addressCaptor.getValue.map(_.asInstanceOf[InternetAddress])
    addressArg.map(_.getAddress).zip(userViews.map(_.email.get)).foreach {
      case (s1: String, s2: String) => s1 should equal(s2)
    }
  }

  private def assertMenuEvent(eventArg: Event, menuEventType: EventType, menu: Menu): Unit = {
    eventArg.`type`.get should equal(menuEventType)
    eventArg.aggregate.get should equal(Menu.aggregateName)
    (eventArg.data.get \ "name").as[String] should equal(menu.name.get)
    (eventArg.data.get \ "recipe").as[String] should equal(menu.recipe.get)
    (eventArg.data.get \ "ingredients").as[Seq[String]].zip(menu.ingredients.get).foreach {
      case (s1: String, s2: String) => s1 should equal(s2)
    }
    (eventArg.data.get \ "link").as[String] should equal(menu.link.get)
  }

  private def assertMenuView(menuViewArg: MenuView, menu: Menu): Unit = {
    menuViewArg.name.get should equal(menu.name.get)
    menuViewArg.ingredients.get.zip(menu.ingredients.get).foreach {
      case (s1: String, s2: String) => s1 should equal(s2)
    }
    menuViewArg.recipe.get should equal(menu.recipe.get)
    menuViewArg.link.get should equal(menu.link.get)
  }
}
