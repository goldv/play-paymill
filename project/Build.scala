import sbt._
import Keys._
import play.Project._

object ApplicationBuild extends Build {

  val appName = "play-paymill"
  val appVersion = "1.0-SNAPSHOT"
  
  val main = play.Project(appName, appVersion).settings(
    organization := "com.goldv")
    

}
