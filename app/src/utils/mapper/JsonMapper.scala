package src.utils.mapper
import java.util.UUID

import akka.http.scaladsl.model.DateTime
import play.api.libs.json._

object JsonMapper {

  implicit val stringSeqWrites = new Writes[Seq[String]] {

    override def writes(stringSeq: Seq[String]): JsValue =
      JsString(stringSeq.mkString(","))
  }

  implicit val stringSeqReads = new Reads[Seq[String]] {

    override def reads(json: JsValue): JsResult[Seq[String]] = json match {
      case JsString(stringSeq) =>
        JsSuccess(stringSeq.split(",").map(_.trim).toSeq)
      case _ =>
        JsError(JsPath(), JsonValidationError("Error parsing Seq[String]"))
    }
  }

  implicit val dateTimeWrites = new Writes[DateTime] {

    override def writes(dateTime: DateTime): JsValue = JsNumber(dateTime.clicks)
  }

  implicit val dateTimeReads = new Reads[DateTime] {

    override def reads(json: JsValue): JsResult[DateTime] = json match {
      case JsNumber(epoch) =>
        JsSuccess(DateTime(epoch.longValue()))
      case _ =>
        JsError(JsPath(), JsonValidationError("Error parsing DateTime"))
    }
  }

  implicit val uuidWrites = new Writes[UUID] {

    override def writes(uuid: UUID): JsValue = JsString(uuid.toString)
  }

  implicit val uuidReads = new Reads[UUID] {

    override def reads(json: JsValue): JsResult[UUID] = json match {
      case JsString(uuid) =>
        JsSuccess(UUID.fromString(uuid))
      case _ =>
        JsError(JsPath(), JsonValidationError("Error parsing UUID"))
    }
  }
}
