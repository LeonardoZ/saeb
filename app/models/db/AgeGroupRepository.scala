package models.db

import javax.inject.Inject

import models.entity.AgeGroup
import play.api.db.slick.DatabaseConfigProvider
import slick.backend.DatabaseConfig
import slick.driver.JdbcProfile

import scala.concurrent.{ExecutionContext, Future}

class AgeGroupRepository @Inject()(protected val tables: Tables,
                                   protected val dbConfigProvider: DatabaseConfigProvider)(implicit ec: ExecutionContext) {

  val dbConfig: DatabaseConfig[JdbcProfile] = dbConfigProvider.get[JdbcProfile]
  val db = dbConfig.db

  import dbConfig.driver.api._

  val AgeGroups = TableQuery[tables.AgeGroupTable]

  def getById(ageGroupId: Int): Future[Option[AgeGroup]] = db.run {
    AgeGroups.filter(_.id === ageGroupId).result.headOption
  }

  def getByGroup(group: String): Future[Option[AgeGroup]] = db.run {
    AgeGroups.filter(_.group === group).result.headOption
  }

  def getAll(): Future[Seq[AgeGroup]] = db.run {
    AgeGroups.result
  }

  def count(): Future[Int] = db.run {
    AgeGroups.countDistinct.result
  }

  def insertAll(ageGroup: Set[AgeGroup]) = db.run {
    (AgeGroups ++= ageGroup).transactionally
  }

  def insertReturningId(ageGroup: AgeGroup): Future[Int] = db.run {
    ((AgeGroups returning AgeGroups.map(_.id) into ((ag, genId) => ag.copy(id = genId))) += ageGroup)
      .map(_.id.getOrElse(0))
  }


}
