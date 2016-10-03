package models.query

import scala.math.BigDecimal.RoundingMode

case class AgeGroupRankingDetails(cityCode: String,
                                   name: String,
                                   state: String,
                                   percent: Double,
                                   peoples: Int,
                                   total: Int) {
  def percentFormatted(): Double = {
    BigDecimal(percent).setScale(4, RoundingMode.HALF_UP).toDouble
  }
}

case class AgeGroupRankingsByLimit(base: Int,
                                    limit: Int,
                                    message: String,
                                    details: Seq[AgeGroupRankingDetails])

case class AgeGroupRankingGroup(group: String, rankingsByLimit: Seq[AgeGroupRankingsByLimit])