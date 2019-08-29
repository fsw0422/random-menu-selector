package utils.db

import cats.effect.IO
import slick.basic.DatabaseConfig
import slick.jdbc.JdbcProfile

trait Db {
  private val dbConfig = DatabaseConfig.forConfig[JdbcProfile]("postgres")

  protected val db = dbConfig.db

  protected implicit val ioExecutor = db.ioExecutionContext

  def setup(): IO[Unit]

  def teardown(): IO[Unit]
}
