package models.query

case class SchoolingComparison(cityNameOne: String,
                               cityOne: Seq[ComparedCitySchooling],
                               cityNameTwo: String,
                               cityTwo: Seq[ComparedCitySchooling])
