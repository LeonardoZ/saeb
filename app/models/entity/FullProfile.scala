package models.entity

import models.query.YearOrMonth

case class Profile(id: Option[Int] = None,
                   year: String = "",
                   month: String = "0",
                   electoralDistrict: String = "",
                   sex: String = "",
                   cityId: Int = 0,
                   ageGroupId: Int = 0,
                   schoolingId: Int = 0,
                   quantityOfPeoples: Int = 0) {

  def yearMonth(): String =
    if (month == 0) year else year + "-" + month

}


case class FullProfile(yearOrMonth: String,
                       city: SimpleCity,
                       electoralDistrict: String,
                       sex: Sex,
                       ageGroup: AgeGroup, schooling: Schooling,
                       quantityOfPeoples: Int)

case class ProfileResult(yearMonths: Seq[String], electoralZones: Seq[Int], totalOfPeoplesByYear: Int)