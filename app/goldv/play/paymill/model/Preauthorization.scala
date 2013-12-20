package goldv.play.paymill.model

import play.api.libs.functional.syntax._
import play.api.libs.json.Reads._
import play.api.libs.json._
import play.api.libs.json.Writes._

case class Preauthorization (id: String)

object Preauthorization{
  implicit val format = Json.format[Preauthorization]
}