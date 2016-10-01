package models.service

import javax.inject.Inject

import models.db.{TaskRepository, UserRepository}
import models.entity.{Task, User}

import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

class TaskService @Inject()(val taskRepository: TaskRepository, val userRepository: UserRepository) {

  import scala.concurrent.ExecutionContext.Implicits.global

  def createTask(description: String, message: String, user: String): Future[Option[Task]] = {
    (for {
      user <- userRepository.getUserUnsafe(user)
      task <- Future { produceTask(description, message, user) }
      taskId <- taskRepository.insertReturningId(task)
    } yield (task, taskId)) map {
      case (task, taskIdTry: Try[Int]) => taskIdTry match {
        case Success(persistedId) => Some(task.copy(id = Some(persistedId)))
        case Failure(ex) => None
      }
    }
  }

  def updateTaskSuccess(task: Task, message: String) =  updateTask(task, message, true, false)

  def updateTaskFailure(task: Task, message: String) = updateTask(task, message, false, true)

  private def updateTask(task: Task, messageP: String, successP: Boolean, failureP: Boolean) = {
    val updatedTask = task.copy(message = messageP, failure = failureP, completed = successP)
    taskRepository.updateTask(updatedTask)
  }

  private def produceTask(description: String, message: String, user: User) = {
    Task(id = None, description = description, message = message, userId = user.id.get)
  }

}
