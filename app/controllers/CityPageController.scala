package controllers

import javax.inject.Inject

import models.db._
import models.entity._
import models.query._
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.libs.json.{JsError, JsResult, Json}
import play.api.mvc.{Action, BodyParsers, Controller}

import scala.collection.immutable.Iterable
import scala.concurrent.{ExecutionContext, Future}


class CityPageController @Inject()(val cityRepository: CityRepository,
                                   val profileRepository: ProfileRepository,
                                   val messagesApi: MessagesApi)(implicit ec: ExecutionContext)
  extends Controller with I18nSupport {

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

    profileCityGroupF.flatMap { profileCityGroup =>

      val byGroup: Map[String, Seq[(Profile, City, AgeGroup)]] = profileCityGroup.groupBy(_._3.group)
      val byGroupAndThenSex =
        byGroup.map { (ageGroupAndProfileValues: (String, Seq[(Profile, City, AgeGroup)])) =>
          (ageGroupAndProfileValues._1, ageGroupAndProfileValues._2.groupBy {
            (profileAndValues: (Profile, City, AgeGroup)) => profileAndValues._1.sex
          })
        }

      val byGroupAndSexSum: Map[String, Map[String, Int]] =
        byGroupAndThenSex.map { (groupAndMapOfSexAndValues: (String, Map[String, Seq[(Profile, City, AgeGroup)]])) =>
          (groupAndMapOfSexAndValues._1, groupAndMapOfSexAndValues._2.mapValues {
            _.map(_._1.quantityOfPeoples).sum
          })
        }

      val profilesByAgeGroups = byGroupAndSexSum.map { (values) =>
        val totalsOfProfiles: Iterable[TotalProfilesBySexUnderGroup] = values._2.map { valuesBySex =>
          TotalProfilesBySexUnderGroup(valuesBySex._1, valuesBySex._2)
        }
        ProfilesByAgeGroup(values._1, totalsOfProfiles.toSeq)
      }.toSeq.filter(_.ageGroup != "INVÁLIDA")

      Future {
        Ok(Json.obj("profiles" -> profilesByAgeGroups.sortBy(_.ageGroup)))
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
    profileCitySchoolingF.flatMap { profileCitySchooling =>
      if (profileCitySchooling.isEmpty)
        Future {
          Ok(Json.obj("profiles" -> Seq[ProfilesBySchooling]()))
        }
      else {
        // group by schooling
        val bySchooling =
        profileCitySchooling.groupBy(v => (v._3.position, v._3.level))

        // then group the values bby sex
        val bySchoolingAndThenSex =
        bySchooling.map { (SchoolingAndProfileValues: ((Int, String), Seq[(Profile, City, Schooling)])) =>
          (SchoolingAndProfileValues._1, SchoolingAndProfileValues._2.groupBy {
            (profileAndValues: (Profile, City, Schooling)) => profileAndValues._1.sex
          })
        }

        // get values for each sex, map and then sum
        val bySchoolingAndSexSum: Map[(Int, String), Map[String, Int]] =
        bySchoolingAndThenSex.map { (SchoolingAndMapOfSexAndValues: ((Int, String), Map[String, Seq[(Profile, City, Schooling)]])) =>
          (SchoolingAndMapOfSexAndValues._1, SchoolingAndMapOfSexAndValues._2.mapValues {
            _.map(_._1.quantityOfPeoples).sum
          })
        }

        // simple map to a more specific type, with some filtering
        val profilesByPositionSchoolings = bySchoolingAndSexSum.map { (values) =>
          val totalsOfProfiles: Iterable[TotalProfilesBySexUnderSchooling] = values._2.map { valuesBySex =>
            TotalProfilesBySexUnderSchooling(valuesBySex._1, valuesBySex._2)
          }
          ProfilesBySchoolingAndPosition(values._1, totalsOfProfiles.toSeq)
        }.toSeq.filter(_.positionAndSchooling._2 != "NÃO INFORMADO").sortBy(_.positionAndSchooling._1)

        val profilesBySchoolings = profilesByPositionSchoolings.map {
          ps => ProfilesBySchooling(ps.positionAndSchooling._2, ps.profilesBySex)
        }

        Future {
          Ok(Json.obj("profiles" -> profilesBySchoolings))
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
      yearCityCode => {
        val profilesF = profileRepository.getProfilesByCityAndYear(yearCityCode.year, yearCityCode.code)
        profilesF flatMap { profiles =>
          if (profiles.isEmpty)
            Future {
              Ok(Json.obj("profiles" -> Map[String, Int]()))
            }
          else {
            val profilesBySex: Seq[ProfileBySex] = profiles.groupBy(_.sex).map { sexAndProfiles =>
              ProfileBySex(sexAndProfiles._1, sexAndProfiles._2.map(_.quantityOfPeoples).sum)
            }.toSeq


            Future {
              Ok(Json.obj("profiles" -> profilesBySex))
            }
          }
        }
      }
    )
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
            val peoplesCount: Seq[PeoplesByYearAndSexGrouped] = result.groupBy(_.yearMonth).map { yearAndPeoples =>
              PeoplesByYearAndSexGrouped(yearAndPeoples._1, yearAndPeoples._2.groupBy(_.sex).map{ peoplesBySex =>
                PeoplesBySex(peoplesBySex._1, peoplesBySex._2.map(_.peoples).sum)
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


}
