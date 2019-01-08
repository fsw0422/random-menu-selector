package src.utils.db

import slick.basic.DatabaseConfig
import slick.jdbc.JdbcProfile

trait Dao {
  protected val dbConfig = DatabaseConfig.forConfig[JdbcProfile]("postgres")

  protected val db = dbConfig.db
}
