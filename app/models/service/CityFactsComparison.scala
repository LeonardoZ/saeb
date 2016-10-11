package models.service

import javax.inject.Inject

import models.db.CityRepository
import models.entity.{AgeGroup, City, Profile, Schooling}
import models.query.{ComparedCity, ComparedCityAgeGroup, ComparedCityFull, ComparedCitySchooling}

import scala.concurrent.{ExecutionContext, Future}
import scala.math.BigDecimal.RoundingMode

class CityFactsComparison @Inject()(cityRepository: CityRepository)(implicit ec: ExecutionContext) {

  def calculateValues(cityCode: String, values: Seq[(Profile, Schooling, AgeGroup)]): Future[ComparedCityFull] = {

    val cityF: Future[City] = cityRepository.getByCode(cityCode).map(_.get)
    val population: Int = values.map(mapToPeoples).sum
    val sexCounts: Future[(Int, Int, Int)] = calculateMaleFemaleNotDefinedValues(values)
    val districtsF: Future[Seq[String]] = extractDistricts(values)
    val schoolingsF: Future[Seq[ComparedCitySchooling]] = createComparedSchoolings(values, population)
    val agesF: Future[Seq[ComparedCityAgeGroup]] = createComparedAgeGroups(values, population)

    for {
      city <- cityF
      (maleCount, femaleCount, notInformedCount) <- sexCounts
      districts <- districtsF
      schoolings <- schoolingsF
      ages <- agesF
    } yield ComparedCityFull(
      comparedCity = ComparedCity(
        cityCode = city.code,
        cityName = city.name,
        male = maleCount,
        female = femaleCount,
        percentOfFemale = percentageOf(femaleCount, population),
        percentOfMale = percentageOf(maleCount, population),
        percentOfNotDefined = percentageOf(notInformedCount, population),
        notDefined = notInformedCount,
        population = population
      ),
      schoolings = schoolings,
      ages = ages,
      districts = districts
    )
  }

  private def createComparedAgeGroups(profilesSchoolingsAges: Seq[(Profile, Schooling, AgeGroup)], population: Int): Future[Seq[ComparedCityAgeGroup]] = {
    Future {
      val schoolingSum: Map[AgeGroup, Int] = profilesSchoolingsAges
        .groupBy{ case (_, _, ageGroup) => ageGroup }
        .map { case (ageGroup, xs: Seq[(Profile, Schooling, AgeGroup)]) =>
          (ageGroup, xs.map(mapToPeoples).sum)
        }.filter {
        case (group, value) => !group.group.contains("INVÁLIDA")
      }
      schoolingSum.map { case (schooling, peoples) =>
        ComparedCityAgeGroup(
          schooling.group,
          peoples,
          percentageOf(peoples, population)
        )
      }.toSeq.sortBy(_.group)
    }
  }

  private def createComparedSchoolings(profilesSchoolingsAges: Seq[(Profile, Schooling, AgeGroup)], population: Int) = {
    Future {
      val schoolingSum: Map[Schooling, Int] = profilesSchoolingsAges
        .groupBy{ case (_, schooling, _) => schooling }
        .map { case (schooling, xs: Seq[(Profile, Schooling, AgeGroup)]) =>
          (schooling, xs.map(mapToPeoples).sum)
        }.filter {
        case (schooling, value) => !schooling.level.contains("NÃO INFORMADO")
      }

      schoolingSum.map { case (schooling, peoples) =>
        ComparedCitySchooling(
          schooling.position,
          schooling.level,
          peoples,
          percentageOf(peoples, population)
        )
      }.toSeq.sortBy(_.order)

    }
  }

  private def extractDistricts(profilesSchoolingsAges: Seq[(Profile, Schooling, AgeGroup)]): Future[Seq[String]] = {
    Future {
      profilesSchoolingsAges.map { case (profile, _, _) => profile.electoralDistrict } distinct
    }
  }

  private def calculateMaleFemaleNotDefinedValues(profilesSchoolingsAges: Seq[(Profile, Schooling, AgeGroup)]) = {
    Future {
      val males = calculatePeoplesForSexPredicate("M", profilesSchoolingsAges)
      val females = calculatePeoplesForSexPredicate("F", profilesSchoolingsAges)
      val notDefined = calculatePeoplesForSexPredicate("N", profilesSchoolingsAges)
      (males, females, notDefined)
    }
  }

  private def calculatePeoplesForSexPredicate(predicateValue: String, xs: Seq[(Profile, Schooling, AgeGroup)]) =
    xs.filter { case (profile, _, _) => profile.sex == predicateValue }
      .map(mapToPeoples).sum

  private def mapToPeoples(xs: (Profile, Schooling, AgeGroup)) =
    xs._1.quantityOfPeoples

  private def percentageOf(aValue: Int, ofTotal: Int) = {
    val value = BigDecimal(aValue)
    val total = BigDecimal(ofTotal)
    ((value / total) * 100).bigDecimal.setScale(2, RoundingMode.CEILING).toDouble
  }

}
