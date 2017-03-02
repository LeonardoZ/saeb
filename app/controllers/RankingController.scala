package controllers

import javax.inject.Inject

import models.db._
import models.entity.{AgeGroupRanking, Cities, SchoolingRanking, SimpleCity}
import models.form.AnalysesForm
import models.query._
import play.api.data.Form
import play.api.data.Forms._
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, Controller}

import scala.concurrent.{ExecutionContext, Future}


class RankingController @Inject()(val schoolingRankingRepository: SchoolingRankingRepository,
                                  val cityRepository: CityRepository,
                                  val schoolingRepository: SchoolingRepository,
                                  val dataImportRepository: DataImportRepository,
                                  val ageGroupRepository: AgeGroupRepository,
                                  val ageGroupRankingRepository: AgeGroupRankingRepository,
                                  val messagesApi: MessagesApi)(implicit ec: ExecutionContext)
  extends Controller with I18nSupport {

  val analysesForm: Form[AnalysesForm] = Form {
    mapping(
      "yearMonth" -> nonEmptyText
    )(AnalysesForm.apply)(AnalysesForm.unapply)
  }

  val betwennRules = Seq[((Int, Int), String)](
    ((0, 10000), "Até 10.000 eleitores apenas"),
    ((10001, 50000), "Entre 10.001 e 50.000 eleitores"),
    ((50001, 100000), "Entre 50.001 e 100.000 eleitores"),
    ((100001, 200000), "Entre 100.001 e 200.000 eleitores"),
    ((200001, 500000), "Entre 200.001 e 500.000 eleitores"),
    ((500001, 1000000), "Entre 500.001 até 1.000.000 eleitores"),
    ((1000000, 9999999), "Acima de 1.000.000 de eleitores")
  )

  def schoolingViewRequest = Action.async { implicit request =>
    analysesForm.bindFromRequest.fold(
      error => {
        Future {
          Redirect(routes.RankingController.schoolingAnalysesPage)
        }
      },
      analyses => schoolingRankingRepository.getYears flatMap { yearMonths =>
        val years = formatYears(yearMonths)

        val selectedYear: Option[(String, String)] = years.filter(_._1 == analyses.yearMonth).headOption
        (for {
          transformed <- schoolingTransformation(analyses.yearMonth)
          rankings <- schoolingViewParser(transformed)
        } yield (rankings)) flatMap { rankings =>

          Future(
            Ok(views.html.schooling_ranking(
              selectedYear.getOrElse(("N/A", "N/A"))._2,
              analysesForm,
              years,
              rankings)))

        }
      })
  }

  def schoolingAnalysesPage = Action.async { implicit request =>
    schoolingRankingRepository.getYears flatMap { yearMonths =>
      val years = formatYears(yearMonths)
      if (years.isEmpty)
        Future(Ok(views.html.no_ranking()))
      else {
        val lastYear = years.headOption.getOrElse(("N/A", "N/A"))._2
        (for {
          transformed <- schoolingTransformation(lastYear)
          rankings <- schoolingViewParser(transformed)
        } yield (rankings)) map { rankings =>
          Ok(views.html.schooling_ranking(lastYear, analysesForm, years, rankings))
        }
      }
    }
  }

  def schoolingViewParser(rankings: Map[Int, Seq[((Int, Int, String), Seq[SchoolingRanking])]]): Future[Seq[SchoolingRankingLevel]] = {
    (for {
      schoolings <- schoolingRepository.getAll
      cities <- cityRepository.getAll
    } yield (schoolings, Cities.citiesToSimpleCity(cities).toSeq)) map {
      case (schoolings, cities) =>
        rankings.map {
          case (schoolingId, values) =>
            val schooling = schoolings.filter {
              _.id.get == schoolingId
            }.head
            val rankingsByLimit = values.map {
              case ((base, limit, message), schoolingRankings) =>
                SchoolingRankingsByLimit(base, limit, message, schoolingRankings.map(toDetail(cities, _)))
            }
            SchoolingRankingLevel(schooling.position, schooling.level, rankingsByLimit)
        }.toSeq.sortBy(_.position)
    }

  }

  def toDetail(cities: Seq[SimpleCity], schoolingRanking: SchoolingRanking) = {
    val city = cities.filter(_.id == schoolingRanking.cityCode).head
    SchoolingRankingDetails(
      cityCode = schoolingRanking.cityCode,
      name = city.name,
      state = city.state,
      percent = schoolingRanking.percentOfTotal,
      peoples = schoolingRanking.peoples,
      total = schoolingRanking.total)
  }

  def schoolingTransformation(yearMonth: String = "2016"): Future[Map[Int, Seq[((Int, Int, String), Seq[SchoolingRanking])]]] = {
    (for {
      foreignCities <- cityRepository.getForeignCities()
      rankings <- schoolingRankingRepository.getAllByYearMonth(yearMonth)
    } yield (foreignCities.map(_.code).toSet, rankings)) map { case (foreignCitiesCode, rankings) =>
      val groupedBySchooling = rankings.groupBy(_.schoolingId)
      groupedBySchooling.map {
        case (id, rankings) =>
          val filtered = betwennRules.map {
            case ((base, limit), message) => {
              val mapped = rankings
                .filter { ranking => foreignCitiesCode.contains(ranking.cityCode) }
                .filter { ranking => ranking.total >= base && ranking.total <= limit }
                .sortBy {
                  _.percentOfTotal
                }(Ordering[Double].reverse)
                .take(10)
              ((base, limit, message), mapped)
            }
          }
          (id, filtered)
      }
    }
  }

  def ageGroupViewRequest = Action.async { implicit request =>
    analysesForm.bindFromRequest.fold(
      error => {
        Future {
          Redirect(routes.RankingController.ageGroupAnalysesPage)
        }
      },
      analyses => ageGroupRankingRepository.getYears flatMap { imports =>
        val years = formatYears(imports)

        val selectedYear = years.filter(_._1 == analyses.yearMonth).head
        (for {
          transformed <- ageGroupTransformation(analyses.yearMonth)
          rankings <- ageGroupViewParser(transformed)
        } yield (rankings)) flatMap { rankings =>
          Future(Ok(views.html.age_group_ranking(
            selectedYear._2,
            analysesForm,
            years,
            rankings))
          )
        }
      })
  }

  def ageGroupAnalysesPage = Action.async { implicit request =>
    ageGroupRankingRepository.getYears flatMap { yearMonths =>
      val years = formatYears(yearMonths)
      if (years.isEmpty)
        Future(Ok(views.html.no_ranking()))
      else {
        val lastYear = years.head._2
        (for {
          transformed <- ageGroupTransformation(lastYear)
          rankings <- ageGroupViewParser(transformed)
        } yield (rankings)) map { rankings =>
          Ok(views.html.age_group_ranking(lastYear, analysesForm, years, rankings))
        }
      }
    }
  }

  def ageGroupViewParser(rankings: Map[Int, Seq[((Int, Int, String), Seq[AgeGroupRanking])]]): Future[Seq[AgeGroupRankingGroup]] = {
    (for {
      ageGroups <- ageGroupRepository.getAll
      cities <- cityRepository.getAll
    } yield (ageGroups, Cities.citiesToSimpleCity(cities).toSeq)) map {
      case (ageGroups, cities) =>
        rankings.map {
          case (ageGroupId, values) =>
            val ageGroup = ageGroups.filter {
              _.id.get == ageGroupId
            }.head
            val rankingsByLimit = values.map {
              case ((base, limit, message), ageGroupRankings) =>
                AgeGroupRankingsByLimit(base, limit, message, ageGroupRankings.map(toDetail(cities, _)))
            }
            AgeGroupRankingGroup(ageGroup.group, rankingsByLimit)
        }.toSeq.sortBy(_.group)
    }

  }

  def toDetail(cities: Seq[SimpleCity], ageGroupRanking: AgeGroupRanking) = {
    val city = cities.filter(_.id == ageGroupRanking.cityCode).head
    AgeGroupRankingDetails(
      cityCode = ageGroupRanking.cityCode,
      name = city.name,
      state = city.state,
      percent = ageGroupRanking.percentOfTotal,
      peoples = ageGroupRanking.peoples,
      total = ageGroupRanking.total)
  }

  def ageGroupTransformation(yearMonth: String = "2016"): Future[Map[Int, Seq[((Int, Int, String), Seq[AgeGroupRanking])]]] = {
    (for {
      foreignCities <- cityRepository.getForeignCities()
      rankings <- ageGroupRankingRepository.getAllByYearMonth(yearMonth)
    } yield (foreignCities.map(_.code).toSet, rankings)) map { case (foreignCitiesCode, rankings) =>
      val groupedByAgeGroup = rankings.groupBy(_.ageGroupId)
      groupedByAgeGroup.map {
        case (id, rankings) =>
          val filtered = betwennRules.map {
            case ((base, limit), message) => {
              val mapped = rankings
                .filter { ranking => foreignCitiesCode.contains(ranking.cityCode) }
                .filter { ranking => ranking.total >= base && ranking.total <= limit }
                .sortBy {
                  _.percentOfTotal
                }(Ordering[Double].reverse)
                .take(10)
              ((base, limit, message), mapped)
            }
          }
          (id, filtered)
      }
    }
  }


}
