package models.query

import play.api.libs.json.JsValue

case class ComparedCitySchooling(level: String, quantity: Int, percentOfTotal: Double)
case class ComparedCityAgeGroup(group: String, quantity: Int, percentOfTotal: Double)

case class ComparedCity(cityCode:String,
                        cityName: String,
                        population: Int,
                        male: Int,
                        female: Int,
                        notDefined: Int)

case class ComparedCityFull(comparedCity: ComparedCity,
                            districts: Seq[String],
                            schoolings: JsValue,
                            ages: JsValue)


case class ComparisonReturn(cityOne: ComparedCityFull, cityTwo: ComparedCityFull)