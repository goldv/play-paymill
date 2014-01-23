package goldv.play.paymill

import goldv.play.paymill.auth.PaymillCredentials
import goldv.play.paymill.auth.PaymillAppCredentials

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
  
  case class PaymillOauthException(error: String, error_description: String) extends Throwable
  
  implicit val authExceptionFormat = Json.format[PaymillOauthException] 
  
  case class PaymillException(status: Int, code: Option[Int]) extends Throwable{
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
      case 100   => "parse error"
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
    
  def transaction(price: Int, currency: String, params: Map[String,Seq[String]])(implicit credentials: PaymillCredentials): Future[Transaction] = {
    val p = params + ( "amount" -> Seq(price.toString) ) + ("currency" -> Seq(currency))
    val result = WS.url("https://api.paymill.com/v2/transactions")
        .withAuth(credentials.secretKey, "", AuthScheme.BASIC)
        .post(p)
    
    result.map{ c => 
      parseResponse[Transaction](c) match{
        case Right(t) => t
        case Left(err) => throw err
      } 
    }
  }
  
  def token(code: String)(implicit credentials: PaymillCredentials, appCredentials: PaymillAppCredentials): Future[AccessToken] = {
    val p = Map( ( "grant_type" -> Seq("authorization_code")), 
                 ("code" -> Seq(code)), 
                 ("client_id" -> Seq(appCredentials.appId)), 
                 ("client_secret" -> Seq(appCredentials.clientSecret)) )
     
    val result = WS.url("https://connect.paymill.com/token").post(p)
        
    result.map{ response =>
      parseAccessToken(response) match{
        case Right(at) => at
        case Left(err) => throw err
      }
    }
  }
  
  private def parseAccessToken(response: Response): Either[PaymillOauthException, AccessToken] = response.status match {
    case status if(status == 200) => {
      Json.fromJson[AccessToken](response.json).map{ token => Right(token)}.recoverTotal{ _ =>
        Left(PaymillOauthException("parse error", s"unable to parse ${response.json}"))
      }
    }
    case _ => {
      Json.fromJson[PaymillOauthException](response.json).map{ error => Left(error)}.recoverTotal{ err =>
        Left(PaymillOauthException("parse error", s"unable to parse error ${response.json}"))
      }
    }
  }
    
  private def parseResponse[T <: PaymillResponse](response: Response)(implicit reader: Reads[T]): Either[PaymillException,T] = response.status match {
    case status if(status == 200) => handleStatusSuccess(response.json,status)(reader)
    case status if(status >= 400) => handleStatusError(response.json, status)(reader)
    case status => Left(PaymillException(status, Some(0) ) )
  }
    
  private def handleStatusSuccess[T <: PaymillResponse](json: JsValue, status: Int)(implicit reader: Reads[T]): Either[PaymillException,T] = {
    convertJson[T](json){ t =>
      if(t.response_code == 20000) Right(t)
      else Left( PaymillException(status, Some(t.response_code)) )
    }
  }
  
  private def handleStatusError[T <: PaymillResponse](json: JsValue, status: Int)(implicit reader: Reads[T]): Either[PaymillException,T] = {
    convertJson[T](json){ t =>
      Left(PaymillException(status,Some( t.response_code ) ))
    }
  }
    
  private def convertJson[T <: PaymillResponse](json: JsValue)(f: T => Either[PaymillException,T])(implicit reader: Reads[T]): Either[PaymillException,T] = {
    json.asOpt[JsObject].map( _ \ "data").flatMap{ o => 
      Json.fromJson[T](o).asOpt.map(Right(_))
    } getOrElse Left(PaymillException(0, Some(100)))
  }
}