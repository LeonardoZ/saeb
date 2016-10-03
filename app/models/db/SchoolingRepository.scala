package models.db

import javax.inject.Inject

import models.entity.Schooling
import play.api.Logger
import play.api.db.slick.DatabaseConfigProvider
import slick.backend.DatabaseConfig
import slick.driver.JdbcProfile

import scala.concurrent.{ExecutionContext, Future}

class SchoolingRepository @Inject()(protected val tables: Tables,
                                    protected val dbConfigProvider: DatabaseConfigProvider)(implicit ec: ExecutionContext) {


  val dbConfig: DatabaseConfig[JdbcProfile] = dbConfigProvider.get[JdbcProfile]
  val db = dbConfig.db

  import dbConfig.driver.api._

  val Schoolings = TableQuery[tables.SchoolingTable]

  def getById(schoolingId: Int): Future[Option[Schooling]] = db.run {
    Schoolings.filter(_.id === schoolingId).result.headOption
  }

  def getByLevel(schoolingLevel: String): Future[Option[Schooling]] = db.run {
    Schoolings.filter(_.level === schoolingLevel).result.headOption
  }

  def getAll(): Future[Seq[Schooling]] = db.run {
    Schoolings.sortBy(_.position.asc).result
  }

  def count(): Future[Int] = db.run {
    Schoolings.countDistinct.result
  }

  def insertAll(schoolings: Iterable[Schooling]) = db.run {
    (Schoolings ++= schoolings).transactionally
  }.recover { case ex => Logger.debug("Error occurred while inserting profile", ex) }

  def update(schooling: Schooling) = db.run {
    val q = for {s <- Schoolings if s.id === schooling.id} yield s.position
    q.update(schooling.position)
  }

  def insertReturningId(schooling: Schooling): Future[Int] = db.run {
    ((Schoolings returning Schoolings.map(_.id) into ((sc, genId) => sc.copy(id = genId))) += schooling)
      .map(_.id.getOrElse(0))
  }


}
