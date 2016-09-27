package controllers

import javax.inject.Inject

import models.db._
import models.entity._
import models.query._
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.libs.json.{JsError, JsResult, Json}
import play.api.mvc.{Action, BodyParsers, Controller}

import scala.concurrent.{ExecutionContext, Future}
import scala.math.BigDecimal.RoundingMode


class CityComparisonController @Inject()(val cityRepository: CityRepository,
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
      val comparedCityOneF = calculateValuesComparison(yearCityCodes.codeOfCityOne, profilesOne)
      val comparedCityTwoF = calculateValuesComparison(yearCityCodes.codeOfCityTwo, profilesTwo)

      for {
        comparedCityOne <- comparedCityOneF
        comparedCityTwo <- comparedCityTwoF
      } yield (Ok(views.html.city_comp_box(Seq(comparedCityOne, comparedCityTwo))))
    }
  }

  def calculateValuesComparison(cityCode: String, values: Seq[(Profile, Schooling, AgeGroup)]):
  Future[ComparedCityFull] =  {
    val cityF: Future[City] = cityRepository.getByCode(cityCode).map(_.get)

    val population: Int = values.map(mapToPeoples).sum

    val sexCounts: Future[(Int, Int, Int)] = calculateMaleFemaleNotDefinedValues(values)

    val districtsF: Future[Seq[String]] = extractDistricts(values)

    val schoolingsF: Future[Seq[ComparedCitySchooling]] = createComparedSchoolings(values, population)

    val agesF: Future[Seq[ComparedCityAgeGroup]] = createComparedAgeGroups(values, population)
    for {
      city <- cityF
      (maleCount, femaleCount, notInformedCount) <- sexCounts
      districts <- districtsF
      schoolings <- schoolingsF
      ages <- agesF
    } yield ComparedCityFull(
              comparedCity = ComparedCity(
                cityCode = city.code,
                cityName = city.name,
                male = maleCount,
                female = femaleCount,
                percentOfFemale = percentageOf(femaleCount, population),
                percentOfMale = percentageOf(maleCount, population),
                percentOfNotDefined = percentageOf(notInformedCount, population),
                notDefined = notInformedCount,
                population = population
              ),
              schoolings = schoolings,
              ages = ages,
              districts = districts
      )
  }

  def createComparedAgeGroups(profilesSchoolingsAges: Seq[(Profile, Schooling, AgeGroup)], population: Int): Future[Seq[ComparedCityAgeGroup]] = {
      Future {
        val schoolingSum: Map[AgeGroup, Int] = profilesSchoolingsAges
          .groupBy{ case (_, _, ageGroup) => ageGroup }
          .map { case (ageGroup, xs: Seq[(Profile, Schooling, AgeGroup)]) =>
            (ageGroup, xs.map(mapToPeoples).sum)
          }.filter {
            case (group, value) => !group.group.contains("INVÁLIDA")
          }
        schoolingSum.map { case (schooling, peoples) =>
          ComparedCityAgeGroup(
            schooling.group,
            peoples,
            percentageOf(peoples, population)
          )
        }.toSeq.sortBy(_.group)
      }
  }

  def createComparedSchoolings(profilesSchoolingsAges: Seq[(Profile, Schooling, AgeGroup)], population: Int) = {
    Future {
      val schoolingSum: Map[Schooling, Int] = profilesSchoolingsAges
          .groupBy{ case (_, schooling, _) => schooling }
          .map { case (schooling, xs: Seq[(Profile, Schooling, AgeGroup)]) =>
                  (schooling, xs.map(mapToPeoples).sum)
          }.filter {
            case (schooling, value) => !schooling.level.contains("NÃO INFORMADO")
          }

      schoolingSum.map { case (schooling, peoples) =>
        ComparedCitySchooling(
          schooling.position,
          schooling.level,
          peoples,
          percentageOf(peoples, population)
        )
      }.toSeq.sortBy(_.order)

    }
  }

  def extractDistricts(profilesSchoolingsAges: Seq[(Profile, Schooling, AgeGroup)]): Future[Seq[String]] = {
    Future {
      profilesSchoolingsAges.map { case (profile, _, _) => profile.electoralDistrict } distinct
    }
  }

  def calculateMaleFemaleNotDefinedValues(profilesSchoolingsAges: Seq[(Profile, Schooling, AgeGroup)]) = {
    Future {
        val males = calculatePeoplesForSexPredicate("M", profilesSchoolingsAges)
        val females = calculatePeoplesForSexPredicate("F", profilesSchoolingsAges)
        val notDefined = calculatePeoplesForSexPredicate("N", profilesSchoolingsAges)
        (males, females, notDefined)
    }
  }

  def calculatePeoplesForSexPredicate(predicateValue: String, xs: Seq[(Profile, Schooling, AgeGroup)]) =
    xs.filter { case (profile, _, _) => profile.sex == predicateValue }
      .map(mapToPeoples).sum

  def mapToPeoples(xs: (Profile, Schooling, AgeGroup)) =
    xs._1.quantityOfPeoples

  def percentageOf(aValue: Int, ofTotal: Int) ={
    val value = BigDecimal(aValue)
    val total = BigDecimal(ofTotal)
    ((value / total) * 100).bigDecimal.setScale(2, RoundingMode.CEILING).toDouble
  }

}
