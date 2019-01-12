package utils.db

import slick.basic.DatabaseConfig
import slick.jdbc.JdbcProfile

trait Dao {
  val dbConfig = DatabaseConfig.forConfig[JdbcProfile]("postgres")

  val db = dbConfig.db
}
