package models

import scala.math.BigDecimal.RoundingMode

package object service {
  def percentageOf(aValue: Int, ofTotal: Int) = {
    val value = BigDecimal(aValue)
    val total = BigDecimal(ofTotal)
    if (value == 0) value.toDouble else ((value / total) * 100).bigDecimal.setScale(2, RoundingMode.CEILING).toDouble
  }
}
