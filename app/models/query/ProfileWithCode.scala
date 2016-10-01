package models.query

case class ProfileWithCode(id: Int,
                           yearOrMonth: String,
                           electoralDistrict: String = "",
                           sex: String = "",
                           quantityOfPeoples: Int = 0,
                           cityId: Int = 0,
                           ageGroupId: Int = 0,
                           schoolingId: Int = 0,
                           cityCode: String
                          )
