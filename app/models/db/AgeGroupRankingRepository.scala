package models.db

import javax.inject.Inject

import models.entity.{AgeGroup, AgeGroupRanking, City}
import models.query.YearOrMonth
import play.api.Logger
import play.api.db.slick.DatabaseConfigProvider
import slick.backend.DatabaseConfig
import slick.driver.JdbcProfile
import slick.jdbc.{GetResult}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

class AgeGroupRankingRepository @Inject()(protected val tables: Tables,
                                          protected val dbConfigProvider: DatabaseConfigProvider)
                                         (implicit ec: ExecutionContext) {

  val dbConfig: DatabaseConfig[JdbcProfile] = dbConfigProvider.get[JdbcProfile]
  val db = dbConfig.db

  import dbConfig.driver.api._

  val AgeGroupRankings = TableQuery[tables.AgeGroupRankingTable]
  val AgeGroups = TableQuery[tables.AgeGroupTable]
  val Cities = TableQuery[tables.CityTable]
  implicit val getYearOrMonth = GetResult(r => YearOrMonth(r.<<))

  def getById(ageGroupId: Int): Future[Option[AgeGroupRanking]] = db.run {
    AgeGroupRankings.filter(_.id === ageGroupId).result.headOption
  }

  def getAll(): Future[Seq[AgeGroupRanking]] = db.run {
    AgeGroupRankings.result
  }


  def getYears(): Future[Vector[YearOrMonth]] = {
    val query =
      sql"""
        select year_or_month from age_group_ranking
        group by year_or_month;
      """.as[YearOrMonth]
    db.run(query)
  }

  def count(): Future[Int] = db.run {
    AgeGroupRankings.countDistinct.result
  }

  def insertAll(ageGroups: Seq[AgeGroupRanking]) = db.run {
    (AgeGroupRankings ++= ageGroups).transactionally
  }.recover { case ex => Logger.debug("Error occurred while inserting age rankings", ex) }

  def remove(yearMonth: String, cityCode:String) = db.run {
    AgeGroupRankings.filter(age => age.cityCode === cityCode && age.yearMonth === yearMonth).delete
  }

  def tryRemove(yearMonth: String, cityCode:String): Future[Try[Int]] = db.run {
    AgeGroupRankings.filter(age => age.cityCode === cityCode && age.yearMonth === yearMonth).delete.asTry
  }

  def remove(yearMonth: String) = db.run {
    AgeGroupRankings.filter(age => age.yearMonth === yearMonth).delete
  }

  def insertReturningId(ageGroup: AgeGroupRanking): Future[Int] = db.run {
    ((AgeGroupRankings returning AgeGroupRankings.map(_.id) into ((ag, genId) => ag.copy(id = genId))) += ageGroup)
      .map(_.id.getOrElse(0))
  }

  def getAllByYearMonthFull(yearMonth: String): Future[Seq[(AgeGroupRanking, City, AgeGroup)]] = db.run {
    val x = for {
      rankings <- (AgeGroupRankings.join(Cities).on {
        case (ranking, city) => ranking.cityCode === city.code
      }.join(AgeGroups).on {
        case ((ranking, city), ageGroup) => ranking.ageGroupId === ageGroup.id
      }).filter {
        case ( (ranking, city), ageGroup) =>
          (ranking.yearMonth === yearMonth)
      }.filterNot {
        case ( (ranking, city), ageGroup) =>
          (city.state === "ZZ")
      }.distinctOn {
        case ( (ranking, city), ageGroup) => (ranking.cityCode, city.code)
      }
    } yield (rankings._1._1, rankings._1._2, rankings._2)

    x.result
  }

  def getAllByYearMonth(yearMonth: String): Future[Seq[AgeGroupRanking]] = db.run {
    AgeGroupRankings.filter(_.yearMonth === yearMonth).result
  }


}
