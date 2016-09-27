package models.db

import javax.inject.Inject

import models.entity.User
import play.api.db.slick.DatabaseConfigProvider
import slick.backend.DatabaseConfig
import slick.driver.JdbcProfile

import scala.concurrent.Future


class UserRepository @Inject()(protected val tables: Tables,
                               protected val dbConfigProvider: DatabaseConfigProvider) {

  val dbConfig: DatabaseConfig[JdbcProfile] = dbConfigProvider.get[JdbcProfile]
  val db = dbConfig.db

  import dbConfig.driver.api._

  private val Users = TableQuery[tables.UserTable]

  def countUsers: Future[Int] = db.run {
    (Users.countDistinct.result)
  }

  def getAll: Future[Seq[User]] = db.run {
    (Users.result)
  }

  def create(newUser: User) = db.run {
    (Users += newUser)
  }

  def getUser(email: String): Future[Option[User]] =
    db.run {
      Users.filter(_.email === email).result.headOption
    }

  def getUserUnsafe(email: String): Future[User] =
    db.run {
      Users.filter(_.email === email).result.head
    }
}
