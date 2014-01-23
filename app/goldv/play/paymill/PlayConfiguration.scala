package goldv.play.paymill

import play.api.Application
import play.api.PlayException


object PlayConfiguration {
  /**
* Utility method to allow the retrieval of a key from the settings. Will throw
* a PlayException when the key could not be found.
*/
  def get(key: String)(implicit app: Application): String = 
    app.configuration.getString(key).getOrElse(throw new PlayException("Configuration error", "Could not find " + key + " in settings"))
    
  def getOpt(key: String)(implicit app: Application): Option[String] = app.configuration.getString(key)
    
}



