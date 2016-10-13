package controllers

import javax.inject.Inject

import models._
import models.db._
import models.entity.{City, Profile, Schooling, _}
import models.query._
import models.service.{AgeGroupService, CityFactsComparison, SchoolingService}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.libs.json.{JsError, JsResult, Json}
import play.api.mvc.{Action, BodyParsers, Controller, Result}

import scala.concurrent.{ExecutionContext, Future}


class CityPageController @Inject()(val cityRepository: CityRepository,
                                   val dataImportRepository: DataImportRepository,
                                   val cityFactsComparison: CityFactsComparison,
                                   val schoolingService: SchoolingService,
                                   val ageGroupService: AgeGroupService,
                                   val profileRepository: ProfileRepository,
                                   val messagesApi: MessagesApi)(implicit ec: ExecutionContext)
  extends Controller with I18nSupport {

  def getProfilesSummaryForLastYear = Action.async(BodyParsers.parse.json) { implicit request =>
    val jsonResult: JsResult[CityCode] = request.body.validate[CityCode](cityCodeReads)
    jsonResult.fold(
    error => {
      Future {
        BadRequest(Json.obj("status" -> 400, "messages" -> JsError.toJson(error)))
      }
    },
    cityCode => loadComparisonValues(cityCode)
    )
  }

  def loadComparisonValues(cityCode: CityCode): Future[Result] = {
    (for {
      years <- dataImportRepository.getAll()
      lastYear <- Future { importsToYearsForView(years).head }
      profileData <- profileRepository.getProfilesFullByCityAndYear(lastYear._1, cityCode.cityCode)
      result <- cityFactsComparison.calculateValues(cityCode.cityCode, profileData)
    } yield (result)) map { cityCompFull =>
      Ok(views.html.city_data_box(cityCompFull))
    }
  }

 // General Chart
  def getPopulationGrowthData = Action.async(BodyParsers.parse.json) { implicit request =>
    val jsonResult: JsResult[CityCode] = request.body.validate[CityCode](cityCodeReads)
    jsonResult.fold(
    error => {
      Future {
        BadRequest(Json.obj("status" -> 400, "messages" -> JsError.toJson(error)))
      }
    },
    cityCode => loadPopulationGrowthData(cityCode)
    )
  }


  def loadPopulationGrowthData(cityCode: CityCode): Future[Result] = {
    profileRepository.countPeoplesByCityOnYears(cityCode.cityCode).flatMap { counts =>
      val ys = filterLastYears(counts.map(_.yearMonth))
      val rx = counts.filter(r => ys.contains(r.yearMonth))

      val growths: Vector[Growth] = rx.zip(rx.tail).map {
        case (past, present) => {
          val range = s"${yearMonthFormat(past.yearMonth)} - ${yearMonthFormat(present.yearMonth)}"
          val value = ((present.peoples - past.peoples).toDouble / past.peoples)
          Growth(range, value)
        }
      }

      Future {
        Ok(Json.obj("profiles" -> growths))
      }
    }
  }

  def getProfilesForYearAndCode = Action.async(BodyParsers.parse.json) { implicit request =>
    val jsonResult: JsResult[YearCityCode] = request.body.validate[YearCityCode](yearCityCodeReads)
    jsonResult.fold(
      error => {
        Future {
          BadRequest(Json.obj("status" -> 400, "messages" -> JsError.toJson(error)))
        }
      },
      yearCityCode => getAgeGroupChartData(yearCityCode)
    )
  }

  def getAgeGroupChartData(yearCityCode: YearCityCode) = {
    val profileCityGroupF = profileRepository.getProfilesForAgeGroups(yearCityCode.year, yearCityCode.code)

    profileCityGroupF.flatMap { (profileCityGroup: Seq[ProfileCityGroup]) =>
      Future {
        Ok(Json.obj("profiles" -> ageGroupService.getAgeGroupChartData(profileCityGroup)))
      }
    }
  }

  def getProfilesAgeGroupUnifiedForYearAndCode = Action.async(BodyParsers.parse.json) { implicit request =>
    val jsonResult: JsResult[YearCityCode] = request.body.validate[YearCityCode](yearCityCodeReads)
    jsonResult.fold(
      error => {
        Future {
          BadRequest(Json.obj("status" -> 400, "messages" -> JsError.toJson(error)))
        }
      },
      yearCityCode => getAgeGroupUnifiedChartData(yearCityCode)
    )
  }

  def getAgeGroupUnifiedChartData(yearCityCode: YearCityCode) = {
    val profileCityGroupF = profileRepository.getProfilesForAgeGroups(yearCityCode.year, yearCityCode.code)

    profileCityGroupF.flatMap { (profileCityGroup: Seq[ProfileCityGroup]) =>
      Future {
        Ok(Json.obj("profiles" -> ageGroupService.getAgeGroupChartUnifiedData(profileCityGroup)))
      }
    }
  }

  def getProfilesBySchoolingForYearAndCode = Action.async(BodyParsers.parse.json) { implicit request =>
    val jsonResult: JsResult[YearCityCode] = request.body.validate[YearCityCode](yearCityCodeReads)
    jsonResult.fold(
      error => {
        Future {
          BadRequest(Json.obj("status" -> 400, "messages" -> JsError.toJson(error)))
        }
      },
      yearCityCode => getSchoolingChartData(yearCityCode)
    )
  }

  def getSchoolingChartData(yearCityCode: YearCityCode) = {
    val profileCitySchoolingF = profileRepository.getProfilesForSchoolings(yearCityCode.year, yearCityCode.code)

    profileCitySchoolingF.flatMap { (profileCitySchooling: Seq[(Profile, City, Schooling)]) =>
      if (profileCitySchooling.isEmpty)
        Future {
          Ok(Json.obj("profiles" -> Seq[ProfilesBySchooling]()))
        }
      else {
        Future {
          Ok(Json.obj("profiles" -> schoolingService.getSchoolingChartData(profileCitySchooling)))
        }
      }
    }
  }

  def getProfilesBySchoolingUnifiedForYearAndCode = Action.async(BodyParsers.parse.json) { implicit request =>
    val jsonResult: JsResult[YearCityCode] = request.body.validate[YearCityCode](yearCityCodeReads)
    jsonResult.fold(
      error => {
        Future {
          BadRequest(Json.obj("status" -> 400, "messages" -> JsError.toJson(error)))
        }
      },
      yearCityCode => getSchoolingUnifiedChartData(yearCityCode)
    )
  }

  def getSchoolingUnifiedChartData(yearCityCode: YearCityCode) = {
    val profileCitySchoolingF = profileRepository.getProfilesForSchoolings(yearCityCode.year, yearCityCode.code)

    profileCitySchoolingF.flatMap { (profileCitySchooling: Seq[(Profile, City, Schooling)]) =>
      if (profileCitySchooling.isEmpty)
        Future {
          Ok(Json.obj("profiles" -> Seq[ProfilesBySchoolingUnified]()))
        }
      else {
        Future {
          Ok(Json.obj("profiles" -> schoolingService.getSchoolingChartDataUnified(profileCitySchooling)))
        }
      }
    }
  }

  def getProfilesBySex = Action.async(BodyParsers.parse.json) { implicit request =>
    val jsonResult: JsResult[YearCityCode] = request.body.validate[YearCityCode](yearCityCodeReads)
    jsonResult.fold(
      error => {
        Future {
          BadRequest(Json.obj("status" -> 400, "messages" -> JsError.toJson(error)))
        }
      },
      yearCityCode => getProfileBySexData(yearCityCode)
    )
  }

  def getProfileBySexData(yearCityCode: YearCityCode) = {
    val profilesF = profileRepository.getProfilesByCityAndYear(yearCityCode.year, yearCityCode.code)
    profilesF flatMap { profiles =>
      if (profiles.isEmpty)
        Future {
          Ok(Json.obj("profiles" -> Map[String, Order]()))
        }
      else {
        val profilesBySex: Seq[ProfileBySex] = profiles.groupBy(_.sex).map {
          case (sex, profiles) =>
            ProfileBySex(sex, profiles.map(_.quantityOfPeoples).sum)
        }.toSeq

        Future {
          Ok(Json.obj("profiles" -> profilesBySex))
        }
      }
    }
  }

  // General Chart
  def getPeoplesByYearAndSex = Action.async(BodyParsers.parse.json) { implicit request =>
    val jsonResult: JsResult[CityCode] = request.body.validate[CityCode](cityCodeReads)
    jsonResult.fold(
      error => {
        Future {
          BadRequest(Json.obj("status" -> 400, "messages" -> JsError.toJson(error)))
        }
      },
      yearCityCode => {
        val profilesF = profileRepository.countPeoplesByCityOnYearsAndSex(yearCityCode.cityCode)
        profilesF flatMap { result =>
          if (result.isEmpty)
            Future {
              Ok(Json.obj("profiles" -> Map[String, Int]()))
            }
          else {
            val resultBy = result.groupBy(_.yearMonth)
            val ys = filterLastYears(resultBy.map(_._1).toSeq)
            val rx = resultBy.filter(r => ys.contains(r._1))

            val peoplesCount: Seq[PeoplesByYearAndSexGrouped] = rx.map {
              case (year, peoplesByYear) =>
                PeoplesByYearAndSexGrouped(year, peoplesByYear.groupBy(_.sex).map {
                  case (sex, peoplesBySex) =>
                    PeoplesBySex(sex, peoplesBySex.map(_.peoples).sum)
                }.toSeq)
            }.toSeq.sortBy(_.yearMonth)

            Future {
              Ok(Json.obj("profiles" -> peoplesCount))
            }
          }
        }
      }
    )
  }

  // General Chart
  def getPeoplesByYear = Action.async(BodyParsers.parse.json) { implicit request =>
    val jsonResult: JsResult[CityCode] = request.body.validate[CityCode](cityCodeReads)
    jsonResult.fold(
      error => {
        Future {
          BadRequest(Json.obj("status" -> 400, "messages" -> JsError.toJson(error)))
        }
      },
      yearCityCode => {
        val profilesF = profileRepository.countPeoplesByCityOnYears(yearCityCode.cityCode)
        profilesF flatMap { (result: Vector[PeoplesByYear]) =>
          if (result.isEmpty)
            Future {
              Ok(Json.obj("profiles" -> Map[String, Int]()))
            }
          else {
            val ys = filterLastYears(result.map(_.yearMonth))
            val rx = result.filter(r => ys.contains(r.yearMonth))
            Future {
              Ok(Json.obj("profiles" -> PeoplesByYearGrouped(rx)))
            }
          }
        }
      }
    )
  }

  def filterLastYears(ys: Seq[String]) = {
    val validYears = ys.map {
      case y if y.length == 4 => ys.filter(_.startsWith(y)).last
      case x if x.length == 6 =>
        if (ys.filter(_.startsWith(x.substring(0, 4))).size == 1) x else ""
    }.filter(!_.isEmpty)
    validYears
  }
  def getQuantityForSchoolingAndAgeGroup = Action.async(BodyParsers.parse.json) { implicit request =>
    val jsonResult: JsResult[YearCityCode] = request.body.validate[YearCityCode](yearCityCodeReads)
    jsonResult.fold(
      error => {
        Future {
          BadRequest(Json.obj("status" -> 400, "messages" -> JsError.toJson(error)))
        }
      },
      yearCityCode => processQuantityForSchoolingAndAgeGroup(yearCityCode)

    )
  }

  def processQuantityForSchoolingAndAgeGroup(yearCityCode: YearCityCode) = {
    val profileSchoolingAge: Future[Seq[(Profile, Schooling, AgeGroup)]] =
      profileRepository.getProfilesFullByCityAndYear(yearCityCode.year, yearCityCode.code)

    profileSchoolingAge.flatMap { vs =>
      val result: Seq[PeoplesInAgeGroupSchooling] = vs.groupBy {case (_, schooling, age) =>
        (schooling.level, age.group)
      }.map { case (levelGroup, profileSchoolingAge) =>
        (levelGroup, (profileSchoolingAge.map {case (profile, _, _)=> profile.quantityOfPeoples}.sum))
      }.map {case ((level, group), peoples) =>
        PeoplesInAgeGroupSchooling(level, group, peoples)
      }.toSeq

      Future(Ok(Json.obj("profiles" -> result)))
    }
  }


}
