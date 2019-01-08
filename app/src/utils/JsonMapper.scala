package src.utils

import java.util.UUID

import akka.http.scaladsl.model.DateTime
import play.api.libs.json._

object JsonMapper {

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
