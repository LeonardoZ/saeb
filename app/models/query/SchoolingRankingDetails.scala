package models.query

import scala.math.BigDecimal.RoundingMode

case class SchoolingRankingDetails(cityCode: String,
                                   name: String,
                                   state: String,
                                   percent: Double,
                                   peoples: Int,
                                   total: Int) {
  def percentFormatted(): Double = {
    BigDecimal(percent).setScale(4, RoundingMode.HALF_UP).toDouble
  }
}

case class SchoolingRankingsByLimit(base: Int,
                                    limit: Int,
                                    message: String,
                                    details: Seq[SchoolingRankingDetails])

case class SchoolingRankingLevel(position:Int, level: String, rankingsByLimit: Seq[SchoolingRankingsByLimit])