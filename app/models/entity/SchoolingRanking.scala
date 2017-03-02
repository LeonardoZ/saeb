package models.entity

case class SchoolingRanking(id: Option[Int] = None,
                            cityCode: String,
                            yearMonth: String,
                            schoolingId: Int,
                            peoples: Int,
                            percentOfTotal: Double,
                            total: Int)