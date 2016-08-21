package models.db

import javax.inject.Inject

import models.entity.Profile
import play.api.Logger
import play.api.db.slick.DatabaseConfigProvider
import slick.backend.DatabaseConfig
import slick.driver.JdbcProfile

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class ProfileRepository @Inject()(protected val tables: Tables,
                                  protected val dbConfigProvider: DatabaseConfigProvider) {

  val dbConfig: DatabaseConfig[JdbcProfile] = dbConfigProvider.get[JdbcProfile]
  val db = dbConfig.db

  import dbConfig.driver.api._

  val Profiles = TableQuery[tables.ProfileTable]

  def insertAll(profiles: List[Profile]): Future[Any] = db.run {
    (Profiles ++= profiles).transactionally.asTry
  }.recover { case ex => Logger.debug("Error occurred while inserting profiles", ex) }

  def insert(profile: Profile) = db.run {
    (Profiles += profile).asTry
  }.recover { case ex => Logger.debug("Error occurred while inserting profile", ex) }

}
