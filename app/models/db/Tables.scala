package models.db

import javax.inject.{Inject, Singleton}

import models.entity._
import play.api.db.slick.DatabaseConfigProvider
import slick.backend.DatabaseConfig
import slick.driver.JdbcProfile

object Tables {

}

@Singleton
class Tables @Inject()(protected val dbConfigProvider: DatabaseConfigProvider) {

  val dbConfig: DatabaseConfig[JdbcProfile] = dbConfigProvider.get[JdbcProfile]
  val db = dbConfig.db

  import dbConfig.driver.api._

  class UserTable(tag: Tag) extends Table[User](tag, "users") {

    def id = column[Option[Int]]("id", O.AutoInc, O.PrimaryKey)

    def email = column[String]("email")

    def password = column[String]("password")

    def active = column[Boolean]("active")

    def * = (id, email, password, active) <> (User.tupled, User.unapply)
  }

  class AgeGroupTable(tag: Tag) extends Table[AgeGroup](tag, "age_groups") {

    def id = column[Option[Int]]("id", O.AutoInc, O.PrimaryKey)

    def group = column[String]("description", O.Length(30))

    def * = (id, group) <> (AgeGroup.tupled, AgeGroup.unapply)
  }


  class SchoolingTable(tag: Tag) extends Table[Schooling](tag, "schooling_levels") {

    def id = column[Option[Int]]("id", O.AutoInc, O.PrimaryKey)

    def level = column[String]("level", O.Length(30))

    def position = column[Int]("position")

    def * = (id, level, position) <> (Schooling.tupled, Schooling.unapply)
  }


  class CityTable(tag: Tag) extends Table[City](tag, "cities") {

    import MyPostgresDriver.api._

    def id = column[Option[Int]]("id", O.AutoInc, O.PrimaryKey)

    def names = column[List[String]]("names")

    def code = column[String]("code", O.Length(7))

    def state = column[String]("state", O.Length(2))

    def country = column[String]("country", O.Length(35))

    def * = (id, names, code, state, country) <> (City.tupled, City.unapply)
  }

  class ProfileTable(tag: Tag) extends Table[Profile](tag, "profiles") {

    def id = column[Option[Int]]("id", O.AutoInc, O.PrimaryKey)

    def year = column[String]("year", O.Length(4))

    def month = column[String]("month", O.Length(2))

    def electoralDistrict = column[String]("electoral_district", O.Length(10))

    def sex = column[String]("sex", O.Length(13))

    def cityId = column[Int]("city_id")

    def ageGroupId = column[Int]("age_group_id")

    def schoolingId = column[Int]("schooling_level_id")

    def quantityOfPeoples = column[Int]("quantity_peoples")

    def * = (id, year, month, electoralDistrict, sex, cityId, ageGroupId, schoolingId, quantityOfPeoples).
      <>(Profile.tupled, Profile.unapply)

  }

  class DataImportTable(tag: Tag) extends Table[DataImport](tag, "data_imports") {

    def id = column[Option[Int]]("id", O.AutoInc, O.PrimaryKey)

    def importDateTime = column[java.sql.Timestamp]("import_date_time")

    def fileName = column[String]("file_name", O.Length(150))

    def fileYear = column[String]("file_year", O.Length(4))

    def fileMonth = column[String]("file_month", O.Length(2))

    def userId = column[Int]("user_id")

    def * = (id, importDateTime, fileName, fileYear, fileMonth, userId) <> (DataImport.tupled, DataImport.unapply)
  }

  class TaskTable(tag: Tag) extends Table[Task](tag, "tasks") {

    def id = column[Option[Int]]("id", O.AutoInc, O.PrimaryKey)

    def description = column[String]("description", O.Length(140))

    def completed = column[Boolean]("completed")

    def failure = column[Boolean]("failure")

    def message = column[String]("message", O.Length(140))

    def userId = column[Int]("user_id")

    def * = (id, description, userId,completed, failure, message) <> (Task.tupled, Task.unapply)
  }


  class AgeGroupRankingTable(tag: Tag) extends Table[AgeGroupRanking](tag, "age_group_rankings") {

    def id = column[Option[Int]]("id", O.AutoInc, O.PrimaryKey)

    def cityCode = column[String]("city_code", O.Length(7))

    def yearMonth = column[String]("year_or_month", O.Length(7))

    def ageGroupId = column[Int]("age_group_id")

    def peoples = column[Int]("peoples")

    def percentageOfTotal = column[Double]("percent_total")

    def total = column[Int]("total")

    def * = (id, cityCode, yearMonth, ageGroupId, peoples, percentageOfTotal, total) <> (AgeGroupRanking.tupled, AgeGroupRanking.unapply)
  }

  class SchoolingRankingTable(tag: Tag) extends Table[SchoolingRanking](tag, "schooling_level_rankings") {

    def id = column[Option[Int]]("id", O.AutoInc, O.PrimaryKey)

    def cityCode = column[String]("city_code", O.Length(7))

    def yearMonth = column[String]("year_or_month", O.Length(7))

    def schoolingId = column[Int]("schooling_level_id")

    def peoples = column[Int]("peoples")

    def percentageOfTotal = column[Double]("percent_total")

    def total = column[Int]("total")

    def * = (id, cityCode, yearMonth, schoolingId, peoples, percentageOfTotal, total) <> (SchoolingRanking.tupled, SchoolingRanking.unapply)
  }

}
