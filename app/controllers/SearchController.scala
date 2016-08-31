package controllers

import javax.inject.Inject

import models.db._
import models.entity._
import models.form.SearchForm
import models.query.{ProfilesByAgeGroup, TotalProfilesBySexUnderGroup, YearCityCode}
import play.api.data.Form
import play.api.data.Forms._
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.libs.json.{JsError, JsPath, JsResult, Json}
import play.api.mvc.{Action, BodyParsers, Controller}

import scala.collection.immutable.Iterable
import scala.concurrent.{ExecutionContext, Future}


class SearchController @Inject()(val cityRepository: CityRepository,
                                 val profileRepository: ProfileRepository,
                                 val messagesApi: MessagesApi)(implicit ec: ExecutionContext)
  extends Controller with I18nSupport {

  implicit val cityReads = Json.reads[SimpleCity]
  implicit val cityWrites = Json.writes[SimpleCity]
  implicit val yearCityCodeReads = Json.reads[YearCityCode]
  implicit val totalProfilesBySexUnderGroupFormat = Json.format[TotalProfilesBySexUnderGroup]
  implicit val totalProfilesBySexUnderGroupWrites = Json.writes[TotalProfilesBySexUnderGroup]
  implicit val profilesByAgeGroupWrites = Json.writes[ProfilesByAgeGroup]
  implicit val profilesByAgeGroupFormat = Json.format[ProfilesByAgeGroup]

  val searchForm: Form[SearchForm] = Form {
    mapping(
      "cityCode" -> nonEmptyText
    )(SearchForm.apply)(SearchForm.unapply)
  }


  def cityPage = Action.async { implicit request =>
    searchForm.bindFromRequest.fold(
      error => Future {
        Redirect(routes.SearchController.main())
      },
      form => cityRepository.getByCode(form.cityCode).flatMap(analyzeACity(_))
    )
  }

  def analyzeACity(aCity: Option[City]) = {
    aCity match {
      case Some(city) => {
        val rs = for {
          profileResult <- cityToAggregatedResults(city)
        } yield (profileResult)
        rs.flatMap(resultData => {
          Future(Ok(views.html.city(city, resultData)))
        })
      }
      case None => Future {
        Redirect(routes.SearchController.main())
      }
    }
  }


  def cityToAggregatedResults(city: City): Future[ProfileResult] = {
    profileRepository.getProfilesForCity(city.code).map { ps =>
      val years: Seq[String] = ps.map(_._1.yearOrMonth).distinct
        .map { y =>
          if (y.length > 4) y.substring(0, 4) + "-" + y.substring(4)
          else y
        }.sorted
      val districtsCodes: Seq[Int] = ps.map(_._1.electoralDistrict).distinct.map(_.toInt).sorted

      ProfileResult(years, districtsCodes, 0)
    }
  }

  def getProfilesForYearAndCode = Action.async(BodyParsers.parse.json) { implicit request =>
    val jsonResult: JsResult[YearCityCode] = request.body.validate[YearCityCode](yearCityCodeReads)
    jsonResult.fold(
      error => {
        Future {
          println(error);
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
        byGroup.map { values =>
          (values._1, values._2.groupBy(_._1.sex))
        }
      val byGroupAndSexSum: Map[String, Map[String, Int]] =
        byGroupAndThenSex.map { (vals: (String, Map[String, Seq[(Profile, City, AgeGroup)]])) =>
          (vals._1, vals._2.mapValues(_.map(_._1.quantityOfPeoples).sum))
        }

      val profilesByAgeGroups = byGroupAndSexSum.map { (values) =>
        val totalsOfProfiles: Iterable[TotalProfilesBySexUnderGroup] = values._2.map { valuesBySex =>
          TotalProfilesBySexUnderGroup(valuesBySex._1, valuesBySex._2)
        }
        ProfilesByAgeGroup(values._1,totalsOfProfiles.toSeq)
      }.toSeq

      Future {
        Ok(Json.obj("profiles" -> profilesByAgeGroups.sortBy(_.ageGroup)))
      }
    }
  }

  def searchContent = Action.async(BodyParsers.parse.json) { implicit request =>
    implicit val contentReads = ((JsPath \ "cityName").read[String])
    val jsonResult: JsResult[String] = request.body.validate[String](contentReads)
    jsonResult.fold(
      error => {
        Future {
          BadRequest(Json.obj("status" -> 400, "messages" -> JsError.toJson(error)))
        }
      },
      content => {
        val cities: Future[Seq[City]] = cityRepository.searchByName(content)
        cities.flatMap { cs =>
          val simpleCities = cs.map(c => SimpleCity(id = c.code, name = c.name, state = c.state))
          Future {
            Ok(Json.obj("results" -> simpleCities))
          }
        }
      }
    )
  }

  def main() = Action.async {
    Future {
      Ok(views.html.saeb(searchForm))
    }
  }

}
