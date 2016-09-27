package models.service

import javax.inject.Inject

import models._
import models.entity.{AgeGroup, City, Profile}
import models.query._

import scala.collection.immutable.Iterable
import scala.concurrent.ExecutionContext

class AgeGroupService @Inject()()(implicit ec: ExecutionContext) {

  val filterValue = "INVÁLIDA"

  def getAgeGroupChartData(profileCityGroup: Seq[ProfileCityGroup]): Seq[ProfilesByAgeGroup] = {

      profileCityGroup.groupBy {
          case (_, _, ageGroup) => ageGroup.group
      }.map {
          case (group, values: Seq[(Profile, City, AgeGroup)]) =>
            (group, values.groupBy { case (profile, _, _) => profile.sex })
      }.map {
        case (group, sexWithValues: SexProfilesGroup) =>
          (group, sexWithValues.mapValues { (xs: Seq[ProfileCityGroup]) =>
              xs.map { case (profile, _, _) => profile.quantityOfPeoples }.sum }
          )
      }.map { case (group, sexWithSum) =>
          val totalsOfProfiles: Iterable[TotalProfilesBySexUnderGroup] = sexWithSum.map { (valuesBySex: (String, Int)) =>
            TotalProfilesBySexUnderGroup(valuesBySex._1, valuesBySex._2)
          }
        ProfilesByAgeGroup(group, totalsOfProfiles.toSeq)
      }.toSeq.filter(_.ageGroup != filterValue).sortBy(_.ageGroup)
  }
}
