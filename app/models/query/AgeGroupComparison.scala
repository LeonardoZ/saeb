package models.query

case class AgeGroupComparison(cityNameOne: String,
                              cityOne: Seq[ComparedCityAgeGroup],
                              cityNameTwo: String,
                              cityTwo: Seq[ComparedCityAgeGroup])
