package model.diagnostics.viewability

import common.Logging
import com.google.common.util.concurrent.AtomicDouble

abstract class Metric extends Logging {

  private lazy val seconds = new AtomicDouble()  
  private lazy val counter = new AtomicDouble()  

  def increment(amount: Double) = {
    seconds.addAndGet(amount)
    counter.addAndGet(1.0)
  }

  def count = {
    counter.doubleValue match {
       case 0.0 => 0.01
       case _ => counter.doubleValue
    }
  }
  
  def secondsInView = {
    seconds.doubleValue match {
       case 0.0 => 0.01
       case _ => seconds.doubleValue
    }
  }

  def reset() = {
    seconds.set(0.0)
    counter.set(0.0)
  }
}

object Top extends model.diagnostics.viewability.Metric
object Bottom extends model.diagnostics.viewability.Metric
object Inline extends model.diagnostics.viewability.Metric
object MPU extends model.diagnostics.viewability.Metric
object firstView extends model.diagnostics.viewability.Metric

