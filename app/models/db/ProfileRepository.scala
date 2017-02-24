package models.db

import javax.inject.Inject

import models.entity.{AgeGroup, City, Profile, Schooling}
import models.query.{PeoplesByYear, PeoplesByYearAndSex, ProfileWithCode}
import play.api.Logger
import play.api.db.slick.DatabaseConfigProvider
import slick.backend.DatabaseConfig
import slick.driver.JdbcProfile
import slick.jdbc.GetResult

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.Try

class ProfileRepository @Inject()(protected val tables: Tables,
                                  protected val dbConfigProvider: DatabaseConfigProvider) {

  val dbConfig: DatabaseConfig[JdbcProfile] = dbConfigProvider.get[JdbcProfile]
  val db = dbConfig.db

  import dbConfig.driver.api._

  val Profiles = TableQuery[tables.ProfileTable]
  val Cities = TableQuery[tables.CityTable]
  val AgeGroups = TableQuery[tables.AgeGroupTable]
  val Schoolings = TableQuery[tables.SchoolingTable]

  implicit val getPeoplesByYearAndSex = GetResult(r => PeoplesByYearAndSex(r.<<, r.<<, r.<<))
  implicit val getPeoplesByYear = GetResult(r => PeoplesByYear(r.<<, r.<<))
  implicit val getProfileWithCode = GetResult(r => ProfileWithCode(r.<<, r.<<, r.<<, r.<<, r.<<, r.<<, r.<<, r.<<, r.<<))


  def insertAll(profiles: List[Profile]): Future[Any] = db.run {
    (Profiles ++= profiles).transactionally.asTry
  }.recover { case ex => Logger.debug("Error occurred while inserting profiles", ex) }


  def getProfilesForCity(cityCode: String): Future[Seq[(Profile, City)]] = {
    val query = for {
      (profile, city) <- (Profiles join Cities on (_.cityId === _.id)).filter(_._2.code === cityCode)
    } yield (profile, city)
    db.run(query.result)
  }


  def getProfilesByCityAndYear(year: String, month: String, cityCode: String): Future[Seq[Profile]] = {
    val query = for {
      profileCity <- (Profiles.join(Cities).on(_.cityId === _.id))
        .filter { case (profile, _) => profile.year === year }
        .filter { case (profile, _) => profile.month === month }
        .filter { case (_, city) => city.code === cityCode }
    } yield (profileCity._1)

    db.run(query.result)
  }

  def getProfilesByCitiesAndYear2(year: String, month: String, citiesIds: Seq[Int]): Future[Seq[(Profile, City)]] = {
    val query = for {
      profileCity <- (Profiles.join(Cities).on(_.cityId === _.id))
        .filter { case (profile, _) => profile.year === year }
        .filter { case (profile, _) => profile.month === month }
        .filter { case (profile, _) => profile.cityId inSet citiesIds }
    } yield (profileCity)

    db.run(query.result)
  }

  def getProfilesByCitiesAndYear(yearMonth: String, citiesIdsFormatted: String): Future[Try[Vector[ProfileWithCode]]] = {
    val query =
      sql"""
           SELECT p.id, concat(p.year, p.month) as yearOrMonth, p.electoral_district as electoralDistrict, p.sex,
           p.quantity_peoples as quantityOfPeoples, p.city_id as cityId, p.age_group_id as ageGroupId,
           p.schooling_id as schoolingId, c.city_code as cityCode from profiles p
           INNER JOIN cities c ON c.id = p.city_id
           WHERE p.year_or_month = $yearMonth AND p.city_id in ($citiesIdsFormatted)
           ORDER BY p.city_id
        """.as[ProfileWithCode]
    db.run(query.asTry)
  }

  def getProfilesFullByCityAndYear(year: String, month: String, cityCode: String)
    : Future[Seq[(Profile, Schooling, AgeGroup)]] = {
    val query = for {
      (((profile, city), schooling), ageGroup) <-
      Profiles
        .join(Cities).on(_.cityId === _.id)
        .join(Schoolings).on {
        case ((profile, city), schooling) =>
          profile.schoolingId === schooling.id
      }
        .join(AgeGroups).on {
        case (((profile, city), schooling), ageGroup) => profile.ageGroupId === ageGroup.id
      }
        .filter {
          case (((profile, _), _), _) => profile.year === year
        }
        .filter {
          case (((profile, _), _), _) => profile.month === month
        }
        .filter {
          case (((_, city), _), _) => city.code === cityCode
        }
    } yield (profile, schooling, ageGroup)
    db.run(query.result)
  }

  def countPeoplesByCityOnYearsAndSex(cityCode: String): Future[Vector[PeoplesByYearAndSex]] = {
    val query = sql"""
      SELECT
        concat(profile.year, profile.month) as yearOrMonth,
        profile.sex,
        sum(profile.quantity_peoples) as total
      FROM
        profiles
      INNER JOIN
        cities
      ON
        city.id = profiles.city_id
      WHERE
        city_code = $cityCode
      GROUP BY
        profile.year_or_month,
        profile.sex
      ORDER BY
        profile.year_or_month;
    """.as[PeoplesByYearAndSex]
    db.run(query)
  }

  def countPeoplesByCityOnYears(cityCode: String): Future[Vector[PeoplesByYear]] = {
    val query = sql"""
      SELECT
        concat(p.year, p.month) as yearOrMonth,
        sum(profile.quantity_peoples) as total
      FROM
        profiles
      INNER JOIN
        city
      ON
        city.id = profile.city_id
      WHERE
        city_code = $cityCode
      GROUP BY
        profile.year_or_month
      ORDER BY
        profile.year_or_month;
    """.as[PeoplesByYear]
    db.run(query)
  }


  def getProfilesForAgeGroups(year: String,month: String, cityCode: String): Future[Seq[(Profile, City, AgeGroup)]] = {
    val query = for {
      ((profile, city), ageGroup) <-
        (Profiles.join(Cities).on(_.cityId === _.id))
          .join(AgeGroups).on {
            case ((profile, city), ageGroup) => profile.ageGroupId === ageGroup.id
          }
          .filter { case ((profile, _), _) =>
            profile.year === year
          }
          .filter { case ((profile, _), _) =>
            profile.month === month
          }
          .filter { case ((_, city), _) =>
            city.code === cityCode
          }
    } yield (profile, city, ageGroup)
    db.run(query.result)
  }

  def getProfilesForSchoolings(year: String, month: String, cityCode: String): Future[Seq[(Profile, City, Schooling)]] = {
    val query = for {
      ((profile, city), schooling) <-
      (Profiles.join(Cities).on(_.cityId === _.id))
        .join(Schoolings).on {
        case ((profile, city), schooling) => profile.schoolingId === schooling.id
      }
        .filter { case ((profile, _), _) =>
          profile.year === year
        }
        .filter { case ((profile, _), _) =>
          profile.month === month
        }
        .filter { case ((_, city), _) =>
          city.code === cityCode
        }
    } yield (profile, city, schooling)
    db.run(query.result)
  }

  def insert(profile: Profile) = db.run {
    (Profiles += profile).asTry
  }.recover { case ex => Logger.debug("Error occurred while inserting profile", ex) }

  def remove(year: String, month: String) = db.run {
    Profiles.filter(_.year === year).filter(_.month === month).delete
  }

  def tryRemove(year: String, month: String) = db.run {
    Profiles.filter(_.year === year).filter(_.month === month).delete.asTry
  }

}
