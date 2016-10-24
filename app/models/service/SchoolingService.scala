package models.service

import javax.inject.Inject

import models._
import models.query._

import scala.concurrent.ExecutionContext

class SchoolingService @Inject()()(implicit ec: ExecutionContext) {

  val filterValue = "NÃO INFORMADO"

  def getSchoolingChartData(profilesCitiesSchoolings: Seq[ProfileCitySchooling]): Seq[ProfilesBySchooling] = {
    val total = profilesCitiesSchoolings.map(_._1.quantityOfPeoples).sum
    // group by schooling
    profilesCitiesSchoolings.groupBy {
      case (_, _, schooling) => (schooling.position, schooling.level)
    }.map {
      case (level, profilesCitiesSchoolings) =>
        (level, profilesCitiesSchoolings.groupBy { case (profile, _, _) => profile.sex })
    }.map {
      case (level, sexProfiles) =>
        (level, sexProfiles.mapValues { (xs: Seq[ProfileCitySchooling]) =>
          xs.map { case (profile, _, _) => profile.quantityOfPeoples }.sum
        })
    }.map { case (level, sexProfilesCount) =>
      val totalsOfProfiles = sexProfilesCount.map {
        case (sex, totalBy) =>
          TotalProfilesBySexUnderSchooling(sex, totalBy, percentageOf(totalBy, total))
      }

      ProfilesBySchoolingAndPosition(level, totalsOfProfiles.toSeq)

    }.toSeq.filter(_.positionAndSchooling._2 != filterValue).sortBy(_.positionAndSchooling._1).map {
      ps => ProfilesBySchooling(ps.positionAndSchooling._2, ps.profilesBySex)
    }
  }

  def getSchoolingChartDataUnified(profilesCitiesSchoolings: Seq[ProfileCitySchooling]) = {
    val total = profilesCitiesSchoolings.map(_._1.quantityOfPeoples).sum

    // group by schooling
    profilesCitiesSchoolings.groupBy {
      case (_, _, schooling) => (schooling.position, schooling.level)
    }.map {
      case (level, profilesCitiesSchoolings) =>
        (level, profilesCitiesSchoolings.map { case (profile, _, _) => profile.quantityOfPeoples }.sum)
    }.toSeq.filter(_._1._2 != filterValue).sortBy(_._1._1).map {
      ps => ProfilesBySchoolingPositionUnified(ps._1, ps._2)
    }.map {
      p => ProfilesBySchoolingUnified(p.positionAndSchooling._2, p.peoples, percentageOf(p.peoples, total))
    }
  }

  def getSchoolingChartDataUnifiedPercent(profilesCitiesSchoolings: Seq[ProfileCitySchooling]) = {
    val cities = profilesCitiesSchoolings.map(_._2)
    val city = cities.head

    val profiles = getSchoolingChartDataUnified(profilesCitiesSchoolings)
    val total = profiles.map(_.peoples).sum
    (city.name, profiles.map(x => ComparedCitySchooling(0, x.schooling, x.peoples, percentageOf(x.peoples, total))))
  }
}
