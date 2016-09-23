package controllers

import javax.inject.Inject

import models.db._
import models.entity.{City, Profile, Schooling, _}
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
  implicit val peoplesInAgeGroupSchoolingWrite = Json.writes[PeoplesInAgeGroupSchooling]
  implicit val peoplesInAgeGroupSchoolingFormat = Json.format[PeoplesInAgeGroupSchooling]

  type ProfileValues = (Profile, Schooling, AgeGroup)

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


  type Group = String
  type Order = Int
  type Level = (Order, String)
  type Sex = String
  type ProfileCityGroup = (Profile, City, AgeGroup)
  type ProfileCitySchooling = (Profile, City, Schooling)
  type GroupProfiles = Map[Group, Seq[ProfileCityGroup]]
  type LevelProfiles = Map[Level, Seq[ProfileCitySchooling]]
  type SexProfilesGroup = Map[Sex, Seq[ProfileCityGroup]]
  type SexProfilesSchooling = Map[Sex, Seq[ProfileCitySchooling]]
  type SexProfilesCount = Map[Sex, Int]

  def getAgeGroupChartData(yearCityCode: YearCityCode) = {
    val profileCityGroupF = profileRepository.getProfilesForAgeGroups(yearCityCode.year, yearCityCode.code)

    profileCityGroupF.flatMap { (profileCityGroup: Seq[ProfileCityGroup]) =>
      type ToGroupMapper = Seq[ProfileCityGroup] => GroupProfiles

      val byGroup: GroupProfiles = profileCityGroup
        .groupBy {
          case (_, _, ageGroup) => ageGroup.group
        }

      val byGroupAndThenSex: Map[Group, SexProfilesGroup] = byGroup
        .map {
          case (group, values: Seq[(Profile, City, AgeGroup)]) =>
            (group, values.groupBy { case (profile, _, _) => profile.sex })
        }

      val byGroupAndSexSum: Map[Group, SexProfilesCount] =
        byGroupAndThenSex.map {
          case (group, sexWithValues: SexProfilesGroup) =>
            (group, sexWithValues
              .mapValues { (xs: Seq[ProfileCityGroup]) =>
                xs.map { case (profile, _, _) => profile.quantityOfPeoples }.sum
              })
        }

      val profilesByAgeGroups = byGroupAndSexSum.map { case (group, sexWithSum) =>
        val totalsOfProfiles: Iterable[TotalProfilesBySexUnderGroup] = sexWithSum.map { (valuesBySex: (String, Int)) =>
          TotalProfilesBySexUnderGroup(valuesBySex._1, valuesBySex._2)
        }
        ProfilesByAgeGroup(group, totalsOfProfiles.toSeq)
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
        val bySchooling: LevelProfiles = {
          profileCitySchooling.groupBy {
            case (_, _, schooling) => (schooling.position, schooling.level)
          }
        }

        // then group the values bby sex
        val bySchoolingAndThenSex: Map[Level, SexProfilesSchooling] = bySchooling.map {
          case (level, profilesCitIesSchoolings) =>
            (level, profilesCitIesSchoolings.groupBy { case (profile, _, _) => profile.sex })
        }

        // get values for each sex, map and then sum
        val bySchoolingAndSexSum: Map[Level, SexProfilesCount] =
          bySchoolingAndThenSex.map {
            case (level, sexProfiles) =>
              (level, sexProfiles.mapValues { (xs: Seq[ProfileCitySchooling]) =>
                xs.map { case (profile, _, _) => profile.quantityOfPeoples }.sum
              })
          }

        // simple map to a more specific type, with some filtering
        val profilesByPositionSchoolings: Seq[ProfilesBySchoolingAndPosition] = bySchoolingAndSexSum.map { case (level, sexProfilesCount) =>
          val totalsOfProfiles: Iterable[TotalProfilesBySexUnderSchooling] = sexProfilesCount.map { valuesBySex =>
            TotalProfilesBySexUnderSchooling(valuesBySex._1, valuesBySex._2)
          }

          ProfilesBySchoolingAndPosition(level, totalsOfProfiles.toSeq)
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
              PeoplesByYearAndSexGrouped(yearAndPeoples._1, yearAndPeoples._2.groupBy(_.sex).map { peoplesBySex =>
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
      val result: Seq[PeoplesInAgeGroupSchooling] = vs.groupBy { (profileSchoolingAge: (Profile, Schooling, AgeGroup)) =>
        (profileSchoolingAge._2.level, profileSchoolingAge._3.group)
      }.map((keyAndProfiles: ((String, String), Seq[(Profile, Schooling, AgeGroup)])) =>
        (keyAndProfiles._1, (keyAndProfiles._2.map(_._1.quantityOfPeoples).sum))
      ).map(result => PeoplesInAgeGroupSchooling(result._1._1, result._1._2, result._2)).toSeq

      Future(Ok(Json.obj("profiles" -> result)))
    }
  }


}
