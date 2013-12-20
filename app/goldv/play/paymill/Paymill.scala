package goldv.play.paymill

import goldv.play.paymill.auth.PaymillCredentials

import play.api._
import play.api.libs.json._
import play.api.{Application,Play,PlayException}
import play.api.libs.ws._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import scala.concurrent.Future
import scala.concurrent.{Future => success}
import com.ning.http.client.Realm.AuthScheme
import play.api.libs.json._
import play.api.libs.functional.syntax._
import goldv.play.paymill.model.Transaction._
import goldv.play.paymill.model._


object Paymill {
  
  case class PaymillError(status: Int, code: Option[Int]){
    val statusDescription = status match{
      case 200 => "ok"
      case 401 => "Unauthorized"
      case 403 => "Transaction Error"
      case 404 => "Not Found"
      case 412 => "Precondition failed"
      case status if(status >= 500) => "Server error"
      case _ => "Unhandled status value"
    }
    
    val reason = code.map{ 
      case 10001 => "General undefined response."
      case 10002 => "Still waiting on something."
      case 20000 => "General success response."
      case 40000 => "General problem with data."
      case 40001 => "General problem with payment data."
      case 40100 => "Problem with credit card data."
      case 40101 => "Problem with cvv."
      case 40102 => "Card expired or not yet valid."
      case 40103 => "Limit exceeded."
      case 40104 => "Card invalid."
      case 40105 => "Expiry date not valid."
      case 40106 => "Credit card brand required."
      case 40200 => "Problem with bank account data."
      case 40201 => "Bank account data combination mismatch."
      case 40202 => "User authentication failed."
      case 40300 => "Problem with 3d secure data."
      case 40301 => "Currency / amount mismatch"
      case 40400 => "Problem with input data."
      case 40401 => "Amount too low or zero."
      case 40402 => "Usage field too long."
      case 40403 => "Currency not allowed."
      case 50000 => "General problem with backend."
      case 50001 => "Country blacklisted."
      case 50100 => "Technical error with credit card."
      case 50101 => "Error limit exceeded."
      case 50102 => "Card declined by authorization system."
      case 50103 => "Manipulation or stolen card."
      case 50104 => "Card restricted."
      case 50105 => "Invalid card configuration data."
      case 50200 => "Technical error with bank account."
      case 50201 => "Card blacklisted."
      case 50300 => "Technical error with 3D secure."
      case 50400 => "Decline because of risk issues."
      case 50500 => "General timeout."
      case 50501 => "Timeout on side of the acquirer."
      case 50502 => "Risk management transaction timeout."
      case 50600 => "Duplicate transaction."
      case _ => ""
    }
  }
  
  val errorWriter = Writes[PaymillError] {
    case pe => {
      val status = pe.code.getOrElse(0)
      Json.obj("status" -> pe.status,
               "code" -> status,
               "statusDescription" -> pe.statusDescription,
               "reason" -> pe.reason)
    }
  }
  
  implicit val format = Json.reads[PaymillError]
  
  def transaction(price: Int, currency: String, params: Map[String,Seq[String]])(implicit credentials: PaymillCredentials): Future[Either[PaymillError, Transaction]] = {
    val p = params + ( "amount" -> Seq(price.toString) ) + ("currency" -> Seq(currency))
    val result = WS.url("https://api.paymill.com/v2/transactions")
        .withAuth(credentials.secretKey, "", AuthScheme.BASIC)
        .post(p)
    result.map{ c => parseResponse[Transaction](c) }
  }
  
  private def parseResponse[T <: PaymillResponse](response: Response)(implicit reader: Reads[T]): Either[PaymillError,T] = {
    Logger.info(s"paymill s: ${response.status} response: ${response.json.toString}" )
    // parse json into StripeToken
    response.status match {
      case status if(status == 200) => handleStatusSuccess(response.json,status)
      case status if(status >= 400) => handleStatusError(response.json, status)
      case status => Left(PaymillError(status, Some(0) ) )
    }
  }
  
  private def handleStatusSuccess[T <: PaymillResponse](json: JsValue, status: Int)(implicit reader: Reads[T]): Either[PaymillError,T] = {
    convertJson[T](json){ t =>
      if(t.response_code == 20000) Right(t)
      else Left( PaymillError(status, Some(t.response_code)) )
    }
  }
  
  private def handleStatusError[T <: PaymillResponse](json: JsValue, status: Int)(implicit reader: Reads[T]): Either[PaymillError,T] = {
    convertJson[T](json){ t =>
      Left(PaymillError(status,Some( t.response_code ) ))
    }
  }
  
  private def convertJson[T <: PaymillResponse](json: JsValue)(f: T => Either[PaymillError,T])(implicit reader: Reads[T]): Either[PaymillError,T] = {
    json.asOpt[JsObject].map( _ \ "data").map{ o => 
      Json.fromJson[T](o).map(f(_)).recoverTotal{ err =>
        Logger.error(s"unable to parse paymill response: $json error: ${JsError.toFlatJson(err)}")
        Left(PaymillError(0,None))
      } 
    } getOrElse Left(PaymillError(0, None))
  }
}