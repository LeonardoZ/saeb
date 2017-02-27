package models.service

import javax.inject.Inject

import models._
import models.entity.{AgeGroup, City, Profile}
import models.query._

import scala.collection.immutable.Iterable

class AgeGroupService @Inject()() {

  val filterValue = "INVÁLIDA"

  def getAgeGroupChartData(profileCityGroup: Seq[ProfileCityGroup]): Seq[ProfilesByAgeGroup] = {
    val total = profileCityGroup.map(_._1.quantityOfPeoples).sum
    profileCityGroup.groupBy {
      case (_, _, ageGroup) => ageGroup.group
    }.map {
      case (group, values: Seq[(Profile, City, AgeGroup)]) =>
        (group, values.groupBy { case (profile, _, _) => profile.sex })
    }.map {
      case (group, sexWithValues: SexProfilesGroup) =>
        (group, sexWithValues.mapValues { (xs: Seq[ProfileCityGroup]) =>
          xs.map { case (profile, _, _) => profile.quantityOfPeoples }.sum
        }
          )
    }.map { case (group, sexWithSum) =>
      val totalsOfProfiles: Iterable[TotalProfilesBySexUnderGroup] = sexWithSum.map { (valuesBySex: (String, Int)) =>
        TotalProfilesBySexUnderGroup(valuesBySex._1, valuesBySex._2, percentageOf(valuesBySex._2, total))
      }
      ProfilesByAgeGroup(group, totalsOfProfiles.toSeq.sortBy(_.sex))
    }.toSeq.filter(_.ageGroup != filterValue).sortBy(_.ageGroup)
  }

  def getAgeGroupChartUnifiedData(profileCityGroup: Seq[ProfileCityGroup]): Seq[ProfilesByAgeGroupUnified] = {
    val total = profileCityGroup.map(_._1.quantityOfPeoples).sum

    profileCityGroup.groupBy {
      case (_, _, ageGroup) => ageGroup.group
    }.map {
      case (group, values: Seq[(Profile, City, AgeGroup)]) =>
        (group, values.map { case (profile, _, _) => profile.quantityOfPeoples }.sum)
    }.map { case (group, peoples) =>
      ProfilesByAgeGroupUnified(group, peoples, percentageOf(peoples, total))
    }.toSeq.filter(_.ageGroup != filterValue).sortBy(_.ageGroup)
  }

  def getAgeGroupChartUnifiedDataPercent(profileCityGroup: Seq[ProfileCityGroup]): (String, Seq[ComparedCityAgeGroup]) = {
    val cities = profileCityGroup.map(_._2)
    val city = cities.head

    val profiles = getAgeGroupChartUnifiedData(profileCityGroup)
    val total = profiles.map(_.peoples).sum
    (city.name, profiles.map(x => ComparedCityAgeGroup(x.ageGroup, x.peoples, percentageOf(x.peoples, total))))
  }
}
