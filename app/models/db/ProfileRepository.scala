package models.db

import javax.inject.Inject

import models.entity.{AgeGroup, City, Profile, Schooling}
import models.query.PeoplesByYearAndSex
import play.api.Logger
import play.api.db.slick.DatabaseConfigProvider
import slick.backend.DatabaseConfig
import slick.driver.JdbcProfile
import slick.jdbc.GetResult

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class ProfileRepository @Inject()(protected val tables: Tables,
                                  protected val dbConfigProvider: DatabaseConfigProvider) {

  val dbConfig: DatabaseConfig[JdbcProfile] = dbConfigProvider.get[JdbcProfile]
  val db = dbConfig.db

  import dbConfig.driver.api._

  val Profiles = TableQuery[tables.ProfileTable]
  val Cities = TableQuery[tables.CityTable]
  val AgeGroups = TableQuery[tables.AgeGroupTable]
  val Schoolings = TableQuery[tables.SchoolingTable]

  def insertAll(profiles: List[Profile]): Future[Any] = db.run {
    (Profiles ++= profiles).transactionally.asTry
  }.recover { case ex => Logger.debug("Error occurred while inserting profiles", ex) }


  def getProfilesForCity(cityCode: String): Future[Seq[(Profile, City)]] = {
    val query = for {
      (profile, city) <- (Profiles join Cities on (_.cityId === _.id)).filter(_._2.code === cityCode)
    } yield (profile, city)
    db.run(query.result)
  }


  def getProfilesByCityAndYear(year: String, cityCode: String): Future[Seq[Profile]] = {
    val query = for {
      profileCity <- (Profiles.join(Cities).on(_.cityId === _.id))
        .filter(_._1.yearOrMonth === year)
        .filter(_._2.code === cityCode)
    } yield (profileCity._1)
    db.run(query.result)
  }

  implicit val getPeoplesByYearAndSex = GetResult(r => PeoplesByYearAndSex(r.<<, r.<<, r.<<))

  def countPeoplesByCityOnYearsAndSex(cityCode: String): Future[Vector[PeoplesByYearAndSex]] = {
    val query = sql"""
      select
        profile.year_or_month,
        profile.sex,
        sum(profile.quantity_of_peoples) as total
      from
        profile
      inner join
        city
      on
        city.id = profile.city_id
      where
        city_code = '71030'
      group by
        profile.year_or_month,
          profile.sex
      order by profile.year_or_month;
    """.as[PeoplesByYearAndSex]
    db.run(query)
  }


  def getProfilesForAgeGroups(year: String, cityCode: String): Future[Seq[(Profile, City, AgeGroup)]] = {
    val query = for {
      profileCityAndGroup <- (Profiles.join(Cities).on(_.cityId === _.id))
        .join(AgeGroups).on(_._1.ageGroupId === _.id)
        .filter(_._1._1.yearOrMonth === year)
        .filter(_._1._2.code === cityCode)
    } yield (profileCityAndGroup._1._1, profileCityAndGroup._1._2, profileCityAndGroup._2)
    db.run(query.result)
  }

  def getProfilesForSchoolings(year: String, cityCode: String): Future[Seq[(Profile, City, Schooling)]] = {
    val query = for {
      profileCityAndSchooling <- (Profiles.join(Cities).on(_.cityId === _.id))
        .join(Schoolings).on(_._1.schoolingId === _.id)
        .filter(_._1._1.yearOrMonth === year)
        .filter(_._1._2.code === cityCode)
        .sortBy(_._2.id)
    } yield (profileCityAndSchooling._1._1, profileCityAndSchooling._1._2, profileCityAndSchooling._2)
    db.run(query.result)
  }

  def insert(profile: Profile) = db.run {
    (Profiles += profile).asTry
  }.recover { case ex => Logger.debug("Error occurred while inserting profile", ex) }

}
