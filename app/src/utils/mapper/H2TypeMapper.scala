package src.utils.mapper
import java.util.UUID

import akka.http.scaladsl.model.DateTime
import play.api.libs.json.{JsValue, Json}

object H2TypeMapper {

  import slick.jdbc.H2Profile.api._

  implicit val jsValueMapper =
    MappedColumnType.base[JsValue, String](
      jsValue => jsValue.toString(),
      str => Json.parse(str)
    )

  implicit val stringSeqMapper =
    MappedColumnType.base[Seq[String], String](
      list => list.mkString(","),
      str => str.split(",").map(_.trim).toSeq
    )

  implicit val dateTimeMapper =
    MappedColumnType.base[DateTime, Long](
      dateTime => dateTime.clicks,
      epoch => DateTime(epoch)
    )

  implicit val uuidMapper =
    MappedColumnType
      .base[UUID, String](uuid => uuid.toString, str => UUID.fromString(str))

}
