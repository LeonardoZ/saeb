package models.db

import javax.inject.Inject

import models.entity.{City, Schooling, SchoolingRanking}
import play.api.Logger
import play.api.db.slick.DatabaseConfigProvider
import slick.backend.DatabaseConfig
import slick.driver.JdbcProfile

import scala.concurrent.{ExecutionContext, Future}

class SchoolingRankingRepository @Inject()(protected val tables: Tables,
                                           protected val dbConfigProvider: DatabaseConfigProvider)(implicit ec: ExecutionContext) {

  val dbConfig: DatabaseConfig[JdbcProfile] = dbConfigProvider.get[JdbcProfile]
  val db = dbConfig.db

  import dbConfig.driver.api._

  val SchoolingRankings = TableQuery[tables.SchoolingRankingTable]
  val Cities = TableQuery[tables.CityTable]
  val Schoolings = TableQuery[tables.SchoolingTable]

  def getById(schoolingId: Int): Future[Option[SchoolingRanking]] = db.run {
    SchoolingRankings.filter(_.id === schoolingId).result.headOption
  }

  def getAll(): Future[Seq[SchoolingRanking]] = db.run {
    SchoolingRankings.result
  }

  def getAllByYearMonth(yearMonth: String): Future[Seq[SchoolingRanking]] = db.run {
    SchoolingRankings.filter(_.yearMonth === yearMonth).result
  }

  def getAllByYearMonthFull(yearMonth: String): Future[Seq[(SchoolingRanking, City, Schooling)]] = db.run {
    val x = for {
      rankings <- (SchoolingRankings.join(Cities).on {
          case (ranking, city) => ranking.cityCode === city.code
        }.join(Schoolings).on {
          case ((ranking, city), schooling) => ranking.schoolingId === schooling.id
        }).filter {
          case ( (ranking, city), schooling) =>
            (ranking.yearMonth === yearMonth)
        }.filterNot {
          case ( (ranking, city), schooling) =>
            (city.state === "ZZ")
        }.distinctOn {
          case ( (ranking, city), schooling) => (ranking.cityCode, city.code)
        }
    } yield (rankings._1._1, rankings._1._2, rankings._2)

    x.result
  }

  def count(): Future[Int] = db.run {
    SchoolingRankings.countDistinct.result
  }

  def insertAll(schooling: Seq[SchoolingRanking]) = db.run {
    (SchoolingRankings ++= schooling).transactionally
  }.recover { case ex => Logger.debug("Error occurred while inserting schooling ranking", ex) }

  def remove(yearMonth: String, cityCode:String) = db.run {
    SchoolingRankings.filter(sch => sch.cityCode === cityCode && sch.yearMonth === yearMonth).delete
  }.recover { case ex => Logger.debug("Error occurred while inserting schooling ranking", ex) }

  def remove(yearMonth: String) = db.run {
    SchoolingRankings.filter(sch => sch.yearMonth === yearMonth).delete
  }

  def tryRemove(yearMonth: String) = db.run {
    SchoolingRankings.filter(sch => sch.yearMonth === yearMonth).delete.asTry
  }

  def insertReturningId(schooling: SchoolingRanking): Future[Int] = db.run {
    ((SchoolingRankings returning SchoolingRankings.map(_.id) into ((ag, genId) => ag.copy(id = genId))) += schooling)
      .map(_.id.getOrElse(0))
  }


}
