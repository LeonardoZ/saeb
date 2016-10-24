package models.query

case class ProfilesBySchooling(schooling: String, profilesBySex: Seq[TotalProfilesBySexUnderSchooling])

case class ProfilesBySchoolingPositionUnified(positionAndSchooling: (Int, String), peoples: Int)

case class ProfilesBySchoolingUnified(schooling: String, peoples: Int, percentOf: Double)

case class ProfilesBySchoolingAndPosition(positionAndSchooling: (Int, String), profilesBySex: Seq[TotalProfilesBySexUnderSchooling])

case class TotalProfilesBySexUnderSchooling(sex: String, peoples: Int, percentOf: Double)