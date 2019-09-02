package utils.db

import com.github.tminglei.slickpg._
import play.api.libs.json.{JsValue, Json}
import slick.basic.{Capability, DatabaseConfig}
import slick.jdbc.{JdbcCapabilities, JdbcProfile}

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
    implicit val playJsonArrayTypeMapper =
      new AdvancedArrayJdbcType[JsValue](pgjson,
        s => utils.SimpleArrayUtils.fromString[JsValue](Json.parse)(s).orNull,
        v => utils.SimpleArrayUtils.mkString[JsValue](_.toString())(v)
      ).to(_.toList)
  }
}

object PostgresProfile extends PostgresProfile
