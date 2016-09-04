package models.query

case class ProfilesBySchooling(schooling: String, profilesBySex: Seq[TotalProfilesBySexUnderSchooling])

case class TotalProfilesBySexUnderSchooling(sex: String, peoples: Int)