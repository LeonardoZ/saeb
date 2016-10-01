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

  def getAllByState(federativeUnit: String) = db.run {
    Cities.filter(_.state === federativeUnit).result
  }

  def getByCode(code: String): Future[Option[City]] = db.run {
    Cities.filter(_.code === code).sortBy(_.name.desc).result.headOption
  }

  def getByName(cityName: String): Future[Option[City]] = db.run {
    Cities.filter(_.name === cityName).result.headOption
  }

  def getByCountry(country: String): Future[Option[City]] = db.run {
    Cities.filter(_.country === country).result.headOption
  }

  def getForeignCities(): Future[Seq[City]] =  db.run {
    Cities.filterNot(_.state === "ZZ").result
  }



  def getAll(): Future[Seq[City]] = db.run {
    Cities.result
  }

  def searchByName(content: String) = {
    val query = for {
      city <- Cities if city.name like s"%$content%"
    } yield city
    db.run(query.distinctOn(_.name).result)
  }

  def getBrazilianCities = getAll().flatMap { cs =>
    Future {
      cs.filter(_.country == "Brazil")
        .groupBy(_.code)
        .size
    }
  }

  def getBrazilianStates(): Future[Int] = getAll().flatMap {
    cs => Future {
      cs.filter(_.country == "Brazil")
        .groupBy(_.state)
        .size
    }
  }


  def insertAll(cities: Set[City]) = db.run {
    (Cities ++= cities).transactionally.asTry
  }.recover { case ex => Logger.debug("Error occurred while inserting cities", ex) }

  def insertReturningId(city: City) = db.run {
    ((Cities returning Cities.map(_.id) into ((ci, genId) => ci.copy(id = genId))) += city)
      .map(_.id.getOrElse(0)).asTry
  }

}
