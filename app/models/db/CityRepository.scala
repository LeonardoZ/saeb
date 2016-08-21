package models.db

import javax.inject.Inject

import models.entity.City
import play.api.Logger
import play.api.db.slick.DatabaseConfigProvider
import slick.backend.DatabaseConfig
import slick.driver.JdbcProfile

import scala.concurrent.{ExecutionContext, Future}

class CityRepository @Inject()(protected val tables: Tables,
                               protected val dbConfigProvider: DatabaseConfigProvider)(implicit ex: ExecutionContext) {

  val dbConfig: DatabaseConfig[JdbcProfile] = dbConfigProvider.get[JdbcProfile]
  val db = dbConfig.db

  import dbConfig.driver.api._

  val Cities = TableQuery[tables.CityTable]

  def getById(cityId: Int): Future[Option[City]] = db.run {
    Cities.filter(_.id === cityId).result.headOption
  }

  def getByName(cityName: String): Future[Option[City]] = db.run {
    Cities.filter(_.name === cityName).result.headOption
  }

  def getByCountry(country: String): Future[Option[City]] = db.run {
    Cities.filter(_.country === country).result.headOption
  }

  def getAll(): Future[Seq[City]] = db.run {
    Cities.result
  }

  def insertAll(cities: Set[City]) = db.run {
    (Cities ++= cities).transactionally.asTry
  }.recover { case ex => Logger.debug("Error occurred while inserting cities", ex) }

  def insertReturningId(city: City) = db.run {
    ((Cities returning Cities.map(_.id) into ((ci, genId) => ci.copy(id = genId))) += city)
      .map(_.id.getOrElse(0)).asTry
  }

}
