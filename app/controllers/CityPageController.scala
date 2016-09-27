package controllers

import javax.inject.Inject

import models._
import models.db._
import models.entity.{City, Profile, Schooling, _}
import models.query._
import models.service.{AgeGroupService, SchoolingService}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.libs.json.{JsError, JsResult, Json}
import play.api.mvc.{Action, BodyParsers, Controller}

import scala.concurrent.{ExecutionContext, Future}


class CityPageController @Inject()(val cityRepository: CityRepository,
                                   val schoolingService: SchoolingService,
                                   val ageGroupService: AgeGroupService,
                                   val profileRepository: ProfileRepository,
                                   val messagesApi: MessagesApi)(implicit ec: ExecutionContext)
  extends Controller with I18nSupport {


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

  def getPeoplesByYear = Action.async(BodyParsers.parse.json) { implicit request =>
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
            val peoplesCount: Seq[PeoplesByYearAndSexGrouped] = result.groupBy(_.yearMonth).map {
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
