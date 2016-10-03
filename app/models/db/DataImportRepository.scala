package models.db

import javax.inject.Inject

import models.entity.{DataImport, Schooling}
import play.api.Logger
import play.api.db.slick.DatabaseConfigProvider
import slick.backend.DatabaseConfig
import slick.driver.JdbcProfile

import scala.concurrent.{ExecutionContext, Future}

class DataImportRepository @Inject()(protected val tables: Tables,
                                     protected val dbConfigProvider: DatabaseConfigProvider)(implicit ex: ExecutionContext) {


  val dbConfig: DatabaseConfig[JdbcProfile] = dbConfigProvider.get[JdbcProfile]
  val db = dbConfig.db

  import dbConfig.driver.api._

  val DataImports = TableQuery[tables.DataImportTable]

  def getById(dataImportId: Int): Future[Option[DataImport]] = db.run {
    DataImports.filter(_.id === dataImportId).result.headOption
  }

  def getByFileNameAndYear(dataImport: DataImport): Future[Option[DataImport]] = db.run {
    DataImports.filter( di =>
      di.fileName === dataImport.fileName &&
      di.fileYear === dataImport.fileYear &&
      di.fileMonth === dataImport.fileMonth
    ).result.headOption
  }

  def getByYearMonth(dataImport: DataImport): Future[Option[DataImport]] = db.run {
    DataImports.filter( di =>
      di.fileYear === dataImport.fileYear &&
      di.fileMonth === dataImport.fileMonth
    ).result.headOption
  }

  def getAll(): Future[Seq[DataImport]] = db.run {
    DataImports.sortBy(_.fileYear).result
  }

  def remove(yearMonth: String) = db.run {
    val year = yearMonth.substring(0, 4)
    val month = yearMonth.substring(4)
    DataImports.filter(_.fileYear === year).filter(_.fileMonth === month).delete
  }

  def insertAll(dataImports: Set[DataImport]) = db.run {
    (DataImports ++= dataImports).transactionally.asTry
  }.recover { case ex => Logger.debug("Error occurred while inserting dataImports", ex) }

  def insertReturningId(dataImport: DataImport) = db.run {
    ((DataImports returning DataImports.map(_.id) into ((di, genId) => di.copy(id = genId))) += dataImport)
      .map(_.id.getOrElse(0)).asTry
  }

}
