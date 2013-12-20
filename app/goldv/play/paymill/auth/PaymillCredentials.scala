package goldv.play.paymill.auth

import java.util.Date
import play.api.Play.current
import goldv.play.paymill.PlayConfiguration
  
trait PaymillCredentials {
  def publishableKey: String
  def secretKey: String
}

object PaymillCredentials {
  
  def apply(publishableKey: String, secretKey: String) = SimplePaymillCredentials(publishableKey, secretKey)
  
  lazy val fromConfiguration: PaymillCredentials = PaymillCredentials(PlayConfiguration("paymill.publishableKey"), PlayConfiguration("paymill.secretKey"))
  
  implicit def implicitPaymillCredentials:PaymillCredentials = fromConfiguration
}

case class SimplePaymillCredentials(publishableKey: String, secretKey: String) extends PaymillCredentials



