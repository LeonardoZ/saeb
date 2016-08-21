package models.db

import java.util.Date
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

  class UserTable(tag: Tag) extends Table[User](tag, "user") {

    def id = column[Option[Int]]("id", O.AutoInc, O.PrimaryKey)

    def email = column[String]("email")

    def password = column[String]("password")

    def remember = column[Boolean]("remember")

    def * = (id, email, password, remember) <> (User.tupled, User.unapply)
  }

  class AgeGroupTable(tag: Tag) extends Table[AgeGroup](tag, "age_group") {

    def id = column[Option[Int]]("id", O.AutoInc, O.PrimaryKey)

    def group = column[String]("group_description", O.Length(30))

    def * = (id, group) <> (AgeGroup.tupled, AgeGroup.unapply)
  }


  class SchoolingTable(tag: Tag) extends Table[Schooling](tag, "schooling") {

    def id = column[Option[Int]]("id", O.AutoInc, O.PrimaryKey)

    def level = column[String]("level", O.Length(30))

    def * = (id, level) <> (Schooling.tupled, Schooling.unapply)
  }

  class CityTable(tag: Tag) extends Table[City](tag, "city") {

    def id = column[Option[Int]]("id", O.AutoInc, O.PrimaryKey)

    def name = column[String]("name", O.Length(45))

    def code = column[String]("city_code", O.Length(7))

    def state = column[String]("state", O.Length(2))

    def country = column[String]("country", O.Length(35))

    def * = (id, name, code, state, country) <> (City.tupled, City.unapply)
  }

  class ProfileTable(tag: Tag) extends Table[Profile](tag, "profile") {

    def id = column[Option[Int]]("id", O.AutoInc, O.PrimaryKey)

    def yearOrMonth = column[String]("year_or_month", O.Length(7))

    def electoralDistrict = column[String]("electoral_district", O.Length(10))

    def sex = column[String]("sex", O.Length(13))

    def cityId = column[Int]("city_id")

    def ageGroupId = column[Int]("age_group_id")

    def schoolingId = column[Int]("schooling_id")

    def quantityOfPeoples = column[Int]("quantity_of_peoples")

    def * = (id, yearOrMonth, electoralDistrict, sex, cityId, ageGroupId, schoolingId, quantityOfPeoples).
      <>(Profile.tupled, Profile.unapply)
  }

  class DataImportTable(tag: Tag) extends Table[DataImport](tag, "data_import") {

    def id = column[Option[Int]]("id", O.AutoInc, O.PrimaryKey)

    def importDateTime = column[java.sql.Date]("import_date_time")

    def fileName = column[String]("file_name", O.Length(150))

    def fileYear = column[String]("file_year", O.Length(4))

    def fileMonth = column[String]("file_month", O.Length(2))

    def * = (id, importDateTime, fileName, fileYear, fileMonth) <> (DataImport.tupled, DataImport.unapply)
  }

}
