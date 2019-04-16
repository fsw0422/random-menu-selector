package utils.db

import cats.effect.IO
import slick.basic.DatabaseConfig
import slick.jdbc.JdbcProfile

trait Db {
  private val dbConfig = DatabaseConfig.forConfig[JdbcProfile]("postgres")
  val db = dbConfig.db

  import utils.db.PostgresProfile.api._

  implicit val executor = db.ioExecutionContext

  def setup(): IO[Unit] = {
    //TODO: this needs to be removed once we make UUID generation to app level
    IO.fromFuture(IO(db.run(sqlu"""CREATE EXTENSION IF NOT EXISTS "pgcrypto"""").map(_ => ())))
  }

  def teardown(): IO[Unit]
}
