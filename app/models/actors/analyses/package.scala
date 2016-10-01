package models.actors

import scala.math.BigDecimal.RoundingMode

package object analyses {
  def percentageOf(aValue: Int, ofTotal: Int) ={
    val value = BigDecimal(aValue)
    val total = BigDecimal(ofTotal)
    ((value / total) * 100).bigDecimal.setScale(6, RoundingMode.CEILING).toDouble
  }
}
