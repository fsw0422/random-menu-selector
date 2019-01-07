package src.utils

trait Dao {
  import slick.jdbc.PostgresProfile.api._

  protected val db = Database.forConfig("postgres")
}
