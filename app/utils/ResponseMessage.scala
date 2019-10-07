package utils

import cats.effect.IO

object ResponseMessage {
  val FAILED = "FAILED"
  val INCORRECT_SCHEMA_VERSION = "INCORRECT_SCHEMA_VERSION"
  val NO_SUCH_IDENTITY = "NO_SUCH_IDENTITY"
  val NO_SUCH_ITEM = "NO_SUCH_ITEM"
  val PARAM_ERROR = "PARAM_ERROR"
  val PARAM_MISSING = "PARAM_MISSING"
  val SUCCESS = "SUCCESS"
  val UNAUTHORIZED = "ACCESS DENIED"

  def returnError[R](errorMessage: String): IO[Either[String, R]] = IO.pure(Left(errorMessage))
}
