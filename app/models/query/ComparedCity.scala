package models.query

case class ComparedCitySchooling(order: Int, level: String, quantity: Int, percentOfTotal: Double)
case class ComparedCityAgeGroup(group: String, quantity: Int, percentOfTotal: Double)

case class ComparedCity(cityCode:String,
                        cityName: String,
                        population: Int,
                        male: Int,
                        percentOfMale: Double,
                        female: Int,
                        percentOfFemale: Double,
                        notDefined: Int,
                        percentOfNotDefined: Double)

case class ComparedCityFull(comparedCity: ComparedCity,
                            districts: Seq[String],
                            schoolings: Seq[ComparedCitySchooling],
                            ages: Seq[ComparedCityAgeGroup])


case class ComparisonReturn(cityOne: ComparedCityFull, cityTwo: ComparedCityFull)