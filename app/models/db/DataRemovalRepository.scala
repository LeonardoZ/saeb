package models.db

import javax.inject.Inject

import play.api.db.slick.DatabaseConfigProvider
import slick.backend.DatabaseConfig
import slick.driver.JdbcProfile


import scala.concurrent.ExecutionContext.Implicits.global

class DataRemovalRepository @Inject()(protected val tables: Tables,
                                      protected val dbConfigProvider: DatabaseConfigProvider) {

  val dbConfig: DatabaseConfig[JdbcProfile] = dbConfigProvider.get[JdbcProfile]
  val db = dbConfig.db

  import dbConfig.driver.api._

  val Profiles = TableQuery[tables.ProfileTable]
  val AgeGroupRankings = TableQuery[tables.AgeGroupRankingTable]
  val SchoolingRankings = TableQuery[tables.SchoolingRankingTable]
  val DataImports = TableQuery[tables.DataImportTable]

  def tryRemoveData(yearMonth: String) = {
    val year = yearMonth.substring(0, 4)
    val month = yearMonth.substring(4)
    db.run {
      (for {
        removeSchoolingRanking <- SchoolingRankings.filter(_.yearMonth === yearMonth).delete
        removeAgeGroupRanking <- AgeGroupRankings.filter(_.yearMonth === yearMonth).delete
        removeProfiles <- Profiles
          .filter(_.year === year)
          .filter(_.month === month)
          .delete
        removeImports <- DataImports.filter(_.fileYear === year).filter(_.fileMonth === month).delete
      } yield ()).asTry.transactionally
    }

  }

}
