package goldv.play.paymill.model

import play.api.libs.functional.syntax._
import play.api.libs.json.Reads._
import play.api.libs.json._
import play.api.libs.json.Writes._

case class Transaction  (
    id : String,
    amount : String,
    origin_amount : Int,
    status : String,
    description : Option[String],
    livemode : Boolean,
    is_fraud : Boolean,
    refunds : Option[List[Refund]],
    currency : String,
    created_at : Int,
    updated_at : Int,
    response_code : Int,
    short_id : Option[String],
    invoices : Option[List[String]],
    payment : CreditCardPayment,
    client : Option[Client],
    preauthorization : Option[Preauthorization],
    fees : Option[List[Fee]],
    app_id : Option[String]
    ) extends PaymillResponse
    
object Transaction{
  implicit val format = Json.format[Transaction]
}