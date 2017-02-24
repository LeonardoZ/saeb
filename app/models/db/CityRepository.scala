package models.db

import javax.inject.Inject

import models.entity.City
import play.api.Logger
import play.api.db.slick.DatabaseConfigProvider
import slick.backend.DatabaseConfig
import slick.driver.JdbcProfile
import slick.jdbc.{GetResult, PositionedResult}

import scala.concurrent.{ExecutionContext, Future}

class CityRepository @Inject()(protected val tables: Tables,
                               protected val dbConfigProvider: DatabaseConfigProvider)(implicit ex: ExecutionContext) {

  val dbConfig: DatabaseConfig[JdbcProfile] = dbConfigProvider.get[JdbcProfile]
  val db = dbConfig.db

  import MyPostgresDriver.api._

  val Cities = TableQuery[tables.CityTable]

  implicit val getByName: AnyRef with GetResult[City] =
    GetResult((r: PositionedResult) => r.<<)

  def getById(cityId: Int): Future[Option[City]] = db.run {
    Cities.filter(_.id === cityId).result.headOption
  }

  def getAllByState(federativeUnit: String) = db.run {
    Cities.filter(_.state === federativeUnit).result
  }

  def getByCode(code: String): Future[Option[City]] = db.run {
    Cities.filter(_.code === code).result.headOption
  }

  def getByName(cityName: String): Future[Vector[City]] = {
    val q = sql""" SELECT id, names, code, state, country FROM cities
         WHERE $cityName = ANY(names) """.as[City]
    db.run(q)
  }

  def getByCountry(country: String): Future[Option[City]] = db.run {
    Cities.filter(_.country === country).result.headOption
  }

  def getForeignCities(): Future[Seq[City]] = db.run {
    Cities.filterNot(_.state === "ZZ").result
  }


  def getAll(): Future[Seq[City]] = db.run {
    Cities.result
  }

  def searchByName(content: String): Future[Vector[City]] = {
    val q = sql""" SELECT id, names, code, state, country FROM cities
         WHERE array_to_string(names, ', ') like %$content% """.as[City]
    db.run(q)
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
    (Cities ++= cities).transactionally
  }.recover { case ex => Logger.debug(s"Error occurred while inserting cities. \n $ex") }

  def update(city: City) = {
    println(s"Im being called \t $city")
    val names = Cities.filter(_.id === city.id).map(x => (x.names))
    println(s"Im was called \t $city")
    try {
      val update = names.update(city.names).transactionally
      db.run(update)
    } catch {
      case e: Exception => {
        println("Got some other kind of exception "+e)
        Future.failed(e)
      }
    }
  }

  def updateAll(cities: Set[City]) = {
    println("nada ")
    val qs = cities.map { city =>
      val names = for {c <- Cities if c.id === city.id} yield city.names
      ((names.update(city.names)).flatMap { updatedRows =>
        if (updatedRows == 0) DBIO.failed(new Exception("0 rows updated"))
        else DBIO.successful(updatedRows)
      }.transactionally)
    }.toSeq
    db.run(DBIO.sequence(qs)).recover { case ex => println(ex) }
  }


  def insertReturningId(city: City) = db.run {
    ((Cities returning Cities.map(_.id) into ((ci, genId) => ci.copy(id = genId))) += city)
      .map(_.id.getOrElse(0)).asTry
  }

}
