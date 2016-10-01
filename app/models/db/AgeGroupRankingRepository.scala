package models.db

import javax.inject.Inject

import models.entity.AgeGroupRanking
import play.api.Logger
import play.api.db.slick.DatabaseConfigProvider
import slick.backend.DatabaseConfig
import slick.driver.JdbcProfile

import scala.concurrent.{ExecutionContext, Future}

class AgeGroupRankingRankingRepository @Inject()(protected val tables: Tables,
                                                 protected val dbConfigProvider: DatabaseConfigProvider)
                                                 (implicit ec: ExecutionContext) {

  val dbConfig: DatabaseConfig[JdbcProfile] = dbConfigProvider.get[JdbcProfile]
  val db = dbConfig.db

  import dbConfig.driver.api._

  val AgeGroupRankings = TableQuery[tables.AgeGroupRankingTable]

  def getById(ageGroupId: Int): Future[Option[AgeGroupRanking]] = db.run {
    AgeGroupRankings.filter(_.id === ageGroupId).result.headOption
  }

  def getAll(): Future[Seq[AgeGroupRanking]] = db.run {
    AgeGroupRankings.result
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

  def remove(yearMonth: String) = db.run {
    AgeGroupRankings.filter(age => age.yearMonth === yearMonth).delete
  }

  def insertReturningId(ageGroup: AgeGroupRanking): Future[Int] = db.run {
    ((AgeGroupRankings returning AgeGroupRankings.map(_.id) into ((ag, genId) => ag.copy(id = genId))) += ageGroup)
      .map(_.id.getOrElse(0))
  }


}
