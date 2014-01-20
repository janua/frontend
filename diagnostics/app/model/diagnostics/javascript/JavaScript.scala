package model.diagnostics.javascript 

import common.Logging
import net.sf.uadetector.service.UADetectorServiceFactory

object JavaScript extends Logging {
  
  val agent = UADetectorServiceFactory.getResourceModuleParser()
  
  private def isHealthCheck(userAgent: String): Boolean = {
    userAgent startsWith "ELB-HealthChecker"
  }

  private def getOperatingSystem(userAgent:String) = {
    val ua = agent.parse(userAgent)
    ua.getOperatingSystem().getFamily.toString match {
      case "OS_X"       => "osx"
      case "IOS"        => "ios"
      case "ANDROID"    => "android"
      case "WINDOWS"    => "windows"
      case "RIMOS"      => "rimos"
      case _            => "unknown"
    }
  }

  def report(queryString: Map[String, Seq[String]], userAgent: String) {
      
      val qs = queryString.map { case (k,v) => k -> v.mkString }
      val osFamily = getOperatingSystem(userAgent).toString

      if (qs.contains("type") && !isHealthCheck(userAgent)) {
        
        qs.get("type") match {
          case Some("js") => Metric.increment(s"js.${osFamily}") 
          case Some("ads") => Metric.increment("ads") 
          case _ => {}
        }
      
      }
    }
} 
