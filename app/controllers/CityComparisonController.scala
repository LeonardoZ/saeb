package controllers

import javax.inject.Inject

import models.db._
import models.query._
import models.service.{AgeGroupService, CityFactsComparison, SchoolingService}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.libs.json.{JsError, JsResult, Json}
import play.api.mvc.{Action, BodyParsers, Controller}

import scala.concurrent.{ExecutionContext, Future}


class CityComparisonController @Inject()(val cityRepository: CityRepository,
                                         val schoolingService: SchoolingService,
                                         val ageGroupService: AgeGroupService,
                                         val cityFactsComparison: CityFactsComparison,
                                         val dataImportRepository: DataImportRepository,
                                         val profileRepository: ProfileRepository,
                                         val messagesApi: MessagesApi)(implicit ec: ExecutionContext)
  extends Controller with I18nSupport {

  def comparisonPage() = Action.async { implicit request =>
    dataImportRepository.getAll map { imports =>
        val years = imports
            .map { data =>
              val year = data.fileYear
              val month = data.fileMonth
              val newMonth = if (month.length == 1) "0" + month else month
              val newYear = if (!month.isEmpty) (year + "-" + newMonth) else year
              val valueId = if (!month.isEmpty) year + newMonth else year
              (newYear, valueId)
            }
      Ok (views.html.city_comp(years))
    }
  }


  def getComparisonForYearAndCodes = Action.async(BodyParsers.parse.json) { implicit request =>
    val jsonResult: JsResult[YearCityCodes] = request.body.validate[YearCityCodes](yearCityCodesReads)
    jsonResult.fold(
      error => {
        Future {
          BadRequest(Json.obj("status" -> 400, "messages" -> JsError.toJson(error)))
        }
      },
      yearCityCodes => processComparison(yearCityCodes)
    )
  }

  def processComparison(yearCityCodes: YearCityCodes) = {
    val yearAndMonth = YearMonth.split(yearCityCodes.yearMonth)
    val profilesCityOne = profileRepository
      .getProfilesFullByCityAndYear(yearAndMonth.year, yearAndMonth.month, yearCityCodes.codeOfCityOne)
    val profilesCityTwo = profileRepository
      .getProfilesFullByCityAndYear(yearAndMonth.year, yearAndMonth.month, yearCityCodes.codeOfCityTwo)

    val profilesCityOneAndTwo = for {
      profilesOne <- profilesCityOne
      profilesTwo <- profilesCityTwo
    } yield (profilesOne, profilesTwo)

    profilesCityOneAndTwo.flatMap { case (profilesOne, profilesTwo) =>
      val comparedCityOneF = cityFactsComparison.calculateValues(yearCityCodes.codeOfCityOne, profilesOne)
      val comparedCityTwoF = cityFactsComparison.calculateValues(yearCityCodes.codeOfCityTwo, profilesTwo)

      for {
        comparedCityOne <- comparedCityOneF
        comparedCityTwo <- comparedCityTwoF
      } yield (Ok(views.html.city_comp_box(yearCityCodes, Seq(comparedCityOne, comparedCityTwo))))
    }
  }

  def getComparisonForSchooling = Action.async(BodyParsers.parse.json) { implicit request =>
    val jsonResult: JsResult[YearCityCodes] = request.body.validate[YearCityCodes](yearCityCodesReads)
    jsonResult.fold(
      error => {
        Future {
          BadRequest(Json.obj("status" -> 400, "messages" -> JsError.toJson(error)))
        }
      },
      yearCityCodes => processComparisonForSchooling(yearCityCodes)
    )
  }

  def processComparisonForSchooling(yearCityCodes: YearCityCodes) = {
    val yearAndMonth = YearMonth.split(yearCityCodes.yearMonth)
    val profilesCityOne =
      profileRepository.getProfilesForSchoolings(yearAndMonth.year, yearAndMonth.month, yearCityCodes.codeOfCityOne)
    val profilesCityTwo =
      profileRepository.getProfilesForSchoolings(yearAndMonth.year, yearAndMonth.month, yearCityCodes.codeOfCityTwo)

    val profilesCityOneAndTwo = for {
      profilesOne <- profilesCityOne
      profilesTwo <- profilesCityTwo
    } yield (profilesOne, profilesTwo)

    profilesCityOneAndTwo.map { case (profilesOne, profilesTwo) =>
      val comparedCityOneF = schoolingService.getSchoolingChartDataUnifiedPercent(profilesOne)
      val comparedCityTwoF = schoolingService.getSchoolingChartDataUnifiedPercent(profilesTwo)
      (comparedCityOneF, comparedCityTwoF)
    }.flatMap {
      case (comparedCityOne, comparedCityTwo) =>
        Future {
          Ok(Json.obj("comparisons" -> SchoolingComparison(
                        comparedCityOne._1,
                        comparedCityOne._2,
                        comparedCityTwo._1,
                        comparedCityTwo._2)
          ))
        }
    }

  }

  def getComparisonForAgeGroup = Action.async(BodyParsers.parse.json) { implicit request =>
    val jsonResult: JsResult[YearCityCodes] = request.body.validate[YearCityCodes](yearCityCodesReads)
    jsonResult.fold(
      error => {
        Future {
          BadRequest(Json.obj("status" -> 400, "messages" -> JsError.toJson(error)))
        }
      },
      yearCityCodes => processComparisonForAgeGroup(yearCityCodes)
    )
  }

  def processComparisonForAgeGroup(yearCityCodes: YearCityCodes) = {
    val yearAndMonth = YearMonth.split(yearCityCodes.yearMonth)

    val profilesCityOne =
      profileRepository.getProfilesForAgeGroups(yearAndMonth.year, yearAndMonth.month, yearCityCodes.codeOfCityOne)
    val profilesCityTwo =
      profileRepository.getProfilesForAgeGroups(yearAndMonth.year, yearAndMonth.month, yearCityCodes.codeOfCityOne)

    val profilesCityOneAndTwo = for {
      profilesOne <- profilesCityOne
      profilesTwo <- profilesCityTwo
    } yield (profilesOne, profilesTwo)

    profilesCityOneAndTwo.map { case (profilesOne, profilesTwo) =>
      val comparedCityOneF = ageGroupService.getAgeGroupChartUnifiedDataPercent(profilesOne)
      val comparedCityTwoF = ageGroupService.getAgeGroupChartUnifiedDataPercent(profilesTwo)
      (comparedCityOneF, comparedCityTwoF)
    }.flatMap {
      case (comparedCityOne, comparedCityTwo) =>
        Future {
          Ok(Json.obj("comparisons" -> AgeGroupComparison(
                        comparedCityOne._1,
                        comparedCityOne._2,
                        comparedCityTwo._1,
                        comparedCityTwo._2)
          ))
        }
    }

  }



}
