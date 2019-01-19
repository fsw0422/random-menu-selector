package auth

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class Auth @Inject()(implicit executionContext: ExecutionContext) {

  def checkPassword(password: Option[String]) = {
    Future(true)
  }
}
