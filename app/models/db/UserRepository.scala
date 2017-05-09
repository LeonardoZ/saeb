package models.db

import javax.inject.Inject

import models.entity.User
import play.api.db.slick.DatabaseConfigProvider
import slick.backend.DatabaseConfig
import slick.driver.JdbcProfile

import scala.concurrent.{ExecutionContext, Future}


class UserRepository @Inject()(protected val tables: Tables,
                               protected val dbConfigProvider: DatabaseConfigProvider)(implicit ec: ExecutionContext) {

  val dbConfig: DatabaseConfig[JdbcProfile] = dbConfigProvider.get[JdbcProfile]
  val db = dbConfig.db

  import dbConfig.driver.api._

  private val Users = TableQuery[tables.UserTable]

  def countUsers: Future[Int] = db.run {
    (Users.filter(_.active === true).countDistinct.result)
  }

  def getAll: Future[Seq[User]] = db.run {
    (Users.filter(_.active === true).result)
  }

  def getAllInactive: Future[Seq[User]] = db.run {
    (Users.filter(_.active === false).result)
  }

  def create(newUser: User) = db.run {
    (Users += newUser)
  }

  def setUserInactive(userEmail: String) = db.run {
    val userActive = for (user <- Users if user.email === userEmail) yield (user.active)
    userActive.update(false)
  }

  def setUserActive(email: String) =  db.run {
    val userActive = for (user <- Users if user.email === email) yield (user.active)
    userActive.update(true)
  }


  def updatePassword(user: User, newPassword: String) = db.run {
    val userPassword = for (user <- Users if user.email === user.email) yield (user.password)
    userPassword.update(newPassword).asTry
  }

  def updatePassword(email: String, newPassword: String) = db.run {
    val userPassword = for (user <- Users if user.email === user.email) yield (user.password)
    userPassword.update(newPassword).asTry
  }

  def getUser(email: String): Future[Option[User]] =
    db.run {
      Users.filter(_.active === true).filter(_.email === email).result.headOption
    }
  def getUserInactive(email: String): Future[Option[User]] =
    db.run {
      Users.filter(_.active === false).filter(_.email === email).result.headOption
    }

  def getUserUnsafe(email: String): Future[User] =
    db.run {
      Users.filter(_.active === true).filter(_.email === email).result.head
    }
}
