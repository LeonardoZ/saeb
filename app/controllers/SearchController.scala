package controllers

import javax.inject.Inject

import models.db._
import models.entity._
import models.form.SearchForm
import models.service.CityFactsComparison
import play.api.data.Form
import play.api.data.Forms._
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.libs.json.{JsError, JsPath, JsResult, Json}
import play.api.mvc.{Action, BodyParsers, Controller}

import scala.concurrent.{ExecutionContext, Future}


class SearchController @Inject()(val cityRepository: CityRepository,
                                 val cityFactsComparison: CityFactsComparison,
                                 val profileRepository: ProfileRepository,
                                 val messagesApi: MessagesApi)(implicit ec: ExecutionContext)
  extends Controller with I18nSupport {

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

  def cityPageWithCode(cityCode: String) = Action.async { implicit request =>
    cityRepository.getByCode(cityCode).flatMap {
        case None => Future {
          Redirect(routes.SearchController.main())
        }
        case Some(city: City) => analyzeACity(Some(city))
      }
  }

  def analyzeACity(aCity: Option[City]) = {
    aCity match {
      case Some(city) => {
        val rs = for {
          profileResult <- cityToAggregatedResults(city)
        } yield (profileResult)
        rs.flatMap { resultData =>
          Future(Ok(views.html.city(city, resultData)))
        }
      }
      case None => Future {
        Redirect(routes.SearchController.main())
      }
    }
  }

  def cityToAggregatedResults(city: City): Future[ProfileResult] = {
    profileRepository.getProfilesForCity(city.code).map { ps =>
      val years: Seq[String] = ps.map(_._1.yearMonth).distinct.sorted
      val districtsCodes: Seq[Int] = ps.map(_._1.electoralDistrict).distinct.map(_.toInt).sorted
      ProfileResult(years, districtsCodes, 0)
    }
  }


  def searchContent = Action.async(BodyParsers.parse.json) {
    implicit request =>
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
          cities.flatMap {
            cs => {
              val simpleCities = Cities.citiesToSimpleCity(cs)

              Future {
                Ok(Json.obj("results" -> simpleCities))
              }
            }
          }
        }
      )
  }

  def about() = Action.async  {
    Future {
      Ok(views.html.about())
    }
  }

  def main() = Action.async {
    Future {
      Ok(views.html.saeb(searchForm))
    }
  }


}
