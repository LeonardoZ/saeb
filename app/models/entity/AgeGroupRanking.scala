package models.entity

case class AgeGroupRanking(id: Option[Int] = None,
                           cityCode: String,
                           yearMonth: String,
                           ageGroupId: Int,
                           peoples: Int,
                           percentOfTotal: Double,
                           total: Int)