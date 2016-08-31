package models.entity

case class Profile(id: Option[Int] = None,
                   yearOrMonth: String = "",
                   electoralDistrict: String = "",
                   sex: String = "",
                   cityId: Int = 0,
                   ageGroupId: Int = 0,
                   schoolingId: Int = 0,
                   quantityOfPeoples: Int = 0)

case class FullProfile(yearOrMonth: String,
                       city: City,
                       electoralDistrict: String,
                       sex: Sex,
                       ageGroup: AgeGroup, schooling: Schooling,
                       quantityOfPeoples: Int)

case class ProfileResult(yearMonths: Seq[String], electoralZones: Seq[Int], totalOfPeoplesByYear: Int)