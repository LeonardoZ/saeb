package models.service

import javax.inject.Inject

import models._
import models.query._

import scala.collection.immutable.Iterable
import scala.concurrent.ExecutionContext

class SchoolingService @Inject()()(implicit ec: ExecutionContext) {

  val filterValue = "NÃO INFORMADO"

  def getSchoolingChartData2(profilesCitiesSchoolings:  Seq[ProfileCitySchooling]) = {
        // group by schooling
        val bySchooling: LevelProfiles = {
          profilesCitiesSchoolings.groupBy {
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
        val profilesByPositionSchoolings: Seq[ProfilesBySchoolingAndPosition] = bySchoolingAndSexSum
          .map { case (level, sexProfilesCount) =>
            val totalsOfProfiles: Iterable[TotalProfilesBySexUnderSchooling] = sexProfilesCount
              .map { case (sex, total) =>
                TotalProfilesBySexUnderSchooling(sex, total)
              }

            ProfilesBySchoolingAndPosition(level, totalsOfProfiles.toSeq)
          }.toSeq.filter(_.positionAndSchooling._2 != "NÃO INFORMADO").sortBy(_.positionAndSchooling._1)

        profilesByPositionSchoolings.map {
          ps => ProfilesBySchooling(ps.positionAndSchooling._2, ps.profilesBySex)
        }
  }

  def getSchoolingChartData(profilesCitiesSchoolings:  Seq[ProfileCitySchooling]): Seq[ProfilesBySchooling] = {
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
        case (sex, total) =>
          TotalProfilesBySexUnderSchooling(sex, total)
      }

      ProfilesBySchoolingAndPosition(level, totalsOfProfiles.toSeq)

    }.toSeq.filter(_.positionAndSchooling._2 != filterValue).sortBy(_.positionAndSchooling._1).map {
      ps => ProfilesBySchooling(ps.positionAndSchooling._2, ps.profilesBySex)
    }
  }

  def getSchoolingChartDataUnified(profilesCitiesSchoolings:  Seq[ProfileCitySchooling]) = {
    // group by schooling
    profilesCitiesSchoolings.groupBy {
      case (_, _, schooling) => (schooling.position, schooling.level)
    }.map {
      case (level, profilesCitiesSchoolings) =>
        (level, profilesCitiesSchoolings.map { case (profile, _, _) => profile.quantityOfPeoples }.sum)
    }.toSeq.filter(_._1._2 != filterValue).sortBy(_._1._1).map {
      ps => ProfilesBySchoolingPositionUnified(ps._1, ps._2)
    }.map {
      p => ProfilesBySchoolingUnified(p.positionAndSchooling._2, p.peoples)
    }
  }
}
