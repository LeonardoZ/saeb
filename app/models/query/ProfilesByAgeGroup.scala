package models.query

case class ProfilesByAgeGroup(ageGroup: String, profilesBySex: Seq[TotalProfilesBySexUnderGroup])

case class ProfilesByAgeGroupUnified(ageGroup: String, peoples: Int, percentOf: Double)

case class TotalProfilesBySexUnderGroup(sex: String, peoples: Int, percentOf: Double)