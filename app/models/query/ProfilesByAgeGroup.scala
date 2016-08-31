package models.query

case class ProfilesByAgeGroup(ageGroup: String, profilesBySex: Seq[TotalProfilesBySexUnderGroup])

case class TotalProfilesBySexUnderGroup(sex: String, peoples: Int)