package models.db

import javax.inject.Inject

import models.entity.Task
import play.api.Logger
import play.api.db.slick.DatabaseConfigProvider
import slick.backend.DatabaseConfig
import slick.driver.JdbcProfile

import scala.concurrent.{ExecutionContext, Future}

class TaskRepository @Inject()(protected val tables: Tables,
                               protected val dbConfigProvider: DatabaseConfigProvider)(implicit ex: ExecutionContext) {

  val dbConfig: DatabaseConfig[JdbcProfile] = dbConfigProvider.get[JdbcProfile]
  val db = dbConfig.db

  import dbConfig.driver.api._

  val Tasks = TableQuery[tables.TaskTable]

  def getById(taskId: Int): Future[Option[Task]] = db.run {
    Tasks.filter(_.id === taskId).result.headOption
  }

  def getAll(): Future[Seq[Task]] = db.run {
    Tasks.result
  }

  def updateTask(task: Task)  = db.run {
    val q = for {t <- Tasks if t.id == task.id} yield  (t.completed, t.failure)
    q.update(task.completed, task.failure)
  }

  def insertAll(tasks: Set[Task]) = db.run {
    (Tasks ++= tasks).transactionally.asTry
  }.recover { case ex => Logger.debug("Error occurred while inserting tasks", ex) }

  def insertReturningId(task: Task) = db.run {
    ((Tasks returning Tasks.map(_.id) into ((di, genId) => di.copy(id = genId))) += task)
      .map(_.id.getOrElse(0)).asTry
  }

}
