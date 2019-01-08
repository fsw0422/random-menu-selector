package src.utils.db

import com.github.tminglei.slickpg._
import slick.basic.Capability
import slick.jdbc.JdbcCapabilities

trait PostgresProfile
    extends ExPostgresProfile
    with PgArraySupport
    with PgDateSupportJoda
    with PgRangeSupport
    with PgHStoreSupport
    with PgPlayJsonSupport
    with PgNetSupport
    with PgSearchSupport
    with PgLTreeSupport {
  // jsonb support is in postgres 9.4.0 onward; for 9.3.x use "json"
  override def pgjson = "jsonb"

  // Add back `capabilities` to enable native `upsert` support; for postgres 9.5+
  override def computeCapabilities: Set[Capability] =
    super.computeCapabilities + JdbcCapabilities.insertOrUpdate

  override val api = CustomMapperApi

  object CustomMapperApi
    extends API
      with ArrayImplicits
      with JodaDateTimeImplicits
      with RangeImplicits
      with HStoreImplicits
      with PlayJsonImplicits
      with NetImplicits
      with SearchImplicits
      with SearchAssistants
      with LTreeImplicits {
    implicit val stringListTypeMapper =
      new SimpleArrayJdbcType[String]("TEXT")
        .to(_.toSeq)
  }
}

object PostgresProfile extends PostgresProfile with Dao {

  import src.utils.db.PostgresProfile.api._

  // initialization of database
  db.run(sqlu"""CREATE EXTENSION pgcrypto""")
}
