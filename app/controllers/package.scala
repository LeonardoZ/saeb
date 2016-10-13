import models.entity.{DataImport, SimpleCity}
import models.form.{NewPasswordForm, SchoolingOrder}
import models.query._
import play.api.libs.json.{Json, Reads}

/**
  * Created by Leonardo on 24/09/2016.
  */
package object controllers {


  implicit val cityReads = Json.reads[SimpleCity]
  implicit val cityWrites = Json.writes[SimpleCity]
  implicit val yearCityCodeReads = Json.reads[YearCityCode]
  implicit val cityCodeReads = Json.reads[CityCode]
  implicit val totalProfilesBySexUnderGroupFormat = Json.format[TotalProfilesBySexUnderGroup]
  implicit val totalProfilesBySexUnderGroupWrites = Json.writes[TotalProfilesBySexUnderGroup]
  implicit val totalProfilesBySexUnderSchoolingFormat = Json.format[TotalProfilesBySexUnderSchooling]
  implicit val totalProfilesBySexUnderSchoolingWrites = Json.writes[TotalProfilesBySexUnderSchooling]
  implicit val profilesByAgeGroupWrites = Json.writes[ProfilesByAgeGroup]
  implicit val profilesByAgeGroupFormat = Json.format[ProfilesByAgeGroup]
  implicit val profilesBySchoolingWrites = Json.writes[ProfilesBySchooling]
  implicit val profilesBySchoolingFormat = Json.format[ProfilesBySchooling]
  implicit val profilesBySexWrites = Json.writes[ProfileBySex]
  implicit val profilesBySexFormat = Json.format[ProfileBySex]
  implicit val peoplesBySexWrite = Json.writes[PeoplesBySex]
  implicit val peoplesBySexFormat = Json.format[PeoplesBySex]
  implicit val peoplesByYearAndSexWrite = Json.writes[PeoplesByYearAndSexGrouped]
  implicit val peoplesByYearAndSexFormat = Json.format[PeoplesByYearAndSexGrouped]
  implicit val peoplesInAgeGroupSchoolingWrite = Json.writes[PeoplesInAgeGroupSchooling]
  implicit val peoplesInAgeGroupSchoolingFormat = Json.format[PeoplesInAgeGroupSchooling]
  implicit val yearCityCodesReads = Json.reads[YearCityCodes]
  implicit val orderFormat = Json.format[SchoolingOrder]
  implicit val newPasswordReads = Json.reads[NewPasswordForm]
  implicit val newPasswordFormat = Json.format[NewPasswordForm]
  implicit val ordersFormat = Reads.seq(orderFormat)
  implicit val peoplesByYearReads = Json.reads[PeoplesByYear]
  implicit val peoplesByYearFormat = Json.format[PeoplesByYear]
  implicit val peoplesByYearGroupedReads = Json.reads[PeoplesByYearGrouped]
  implicit val peoplesByYearGroupedFormat = Json.format[PeoplesByYearGrouped]
  implicit val profilesBySchoolingUnifiedReads = Json.reads[ProfilesBySchoolingUnified]
  implicit val profilesBySchoolingUnifiedFormat = Json.format[ProfilesBySchoolingUnified]
  implicit val profilesByAgeGroupUnifiedReads = Json.reads[ProfilesByAgeGroupUnified]
  implicit val profilesByAgeGroupUnifiedFormat = Json.format[ProfilesByAgeGroupUnified]
  implicit val growthReads = Json.reads[Growth]
  implicit val growthFormat = Json.format[Growth]

  def importsToYearsForView(dataImports: Seq[DataImport]) = dataImports
    .map { data =>
      val year = data.fileYear
      val month = data.fileMonth
      val newMonth = if (month.length == 1) "0" + month else month
      val newYear = if (!month.isEmpty) (year + "-" + newMonth) else year
      val valueId = if (!month.isEmpty) year + newMonth else year
      (valueId, newYear)
    }.sortBy(_._2).reverse

  def yearMonthFormat(year: String) = if (year.length > 4) year.substring(0, 4) + "-" + year.substring(4) else year


}
