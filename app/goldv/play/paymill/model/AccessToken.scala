package goldv.play.paymill.model

import play.api.libs.functional.syntax._
import play.api.libs.json.Reads._
import play.api.libs.json._
import play.api.libs.json.Writes._

case class AccessToken (
    access_token: String,
    token_type: String,
    refresh_token: String,
    public_key: String,
    merchant_id: String,
    currencies: Seq[String],
    methods: Seq[String])
    
object AccessToken{
  implicit val format = Json.format[AccessToken]
}