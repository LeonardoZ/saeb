package models.db

import javax.inject.Inject

import models.entity.User
import play.api.db.slick.DatabaseConfigProvider
import slick.backend.DatabaseConfig
import slick.driver.JdbcProfile

import scala.concurrent.Future

/**
  * Created by Leonardo on 15/07/2016.
  */
class UserRepository @Inject()(protected val tables: Tables,
                               protected val dbConfigProvider: DatabaseConfigProvider) {

  val dbConfig: DatabaseConfig[JdbcProfile] = dbConfigProvider.get[JdbcProfile]
  val db = dbConfig.db
  import dbConfig.driver.api._

  private val Users = TableQuery[tables.UserTable]



  def create(newUser: User) = db.run {
    (Users += newUser)
  }

  def getUser(email: String): Future[Option[User]] =
    db.run {Users.filter(_.email === email).result.headOption}
}
