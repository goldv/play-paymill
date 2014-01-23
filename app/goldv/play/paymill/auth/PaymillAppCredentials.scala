package goldv.play.paymill.auth

import java.util.Date
import play.api.Play.current
import goldv.play.paymill.PlayConfiguration
  
trait PaymillAppCredentials {
  def appId: String
  def clientSecret: String
  def hashToken: Option[String]
}

object PaymillAppCredentials {
  
  def apply(appId: String, clientSecret: String, hashToken: Option[String]) = SimplePaymillAppCredentials(appId, clientSecret, hashToken)
  
  lazy val fromConfiguration: PaymillAppCredentials = PaymillAppCredentials(PlayConfiguration.get("paymill.app.appId"), PlayConfiguration.get("paymill.app.clientSecret"), PlayConfiguration.getOpt("paymill.app.hashToken"))
  
  implicit def implicitPaymillCredentials:PaymillAppCredentials = fromConfiguration
}

case class SimplePaymillAppCredentials(appId: String, clientSecret: String, hashToken: Option[String]) extends PaymillAppCredentials

