package controllers

import javax.inject.Inject

import models.db._
import models.query._
import models.service.CityFactsComparison
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.libs.json.{JsError, JsResult, Json}
import play.api.mvc.{Action, BodyParsers, Controller}

import scala.concurrent.{ExecutionContext, Future}


class CityComparisonController @Inject()(val cityRepository: CityRepository,
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
    val profilesCityOne = profileRepository.getProfilesFullByCityAndYear(yearCityCodes.year, yearCityCodes.codeOfCityOne)
    val profilesCityTwo = profileRepository.getProfilesFullByCityAndYear(yearCityCodes.year, yearCityCodes.codeOfCityTwo)

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
      } yield (Ok(views.html.city_comp_box(Seq(comparedCityOne, comparedCityTwo))))
    }
  }


}
