package goldv.play.paymill.model

import play.api.libs.functional.syntax._
import play.api.libs.json.Reads._
import play.api.libs.json._
import play.api.libs.json.Writes._

case class CreditCardPayment (
    id: String)
    
    
object CreditCardPayment{
  implicit val format = Json.format[CreditCardPayment]
}