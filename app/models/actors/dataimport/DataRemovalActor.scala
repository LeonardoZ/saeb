package models.actors.dataimport

import akka.actor.{Actor, ActorRef}
import com.google.inject.Inject
import models.db._
import models.entity.Task
import models.service.TaskService
import play.api.libs.concurrent.InjectedActorSupport

import scala.concurrent.Future
import scala.util.{Failure, Success}

object DataRemovalActor {

  trait Factory {
    def apply(): Actor
  }

  case class RemoveData(managerActor: ActorRef, yearMonth: String, task: Task)

}

class DataRemovalActor @Inject()(val taskService: TaskService,
                                 val dataRemovalRepository: DataRemovalRepository)
  extends Actor with InjectedActorSupport {

  import DataRemovalActor._

  import play.api.libs.concurrent.Execution.Implicits._

  override def receive: Receive = {
    case RemoveData(managerActor, yearMonth, task) => {
     dataRemovalRepository.tryRemoveData(yearMonth) map {
       case Success(result) => completeTask(yearMonth, managerActor, task)
       case Failure(ex) => completeFailedTask(yearMonth, managerActor, task)
      }
    }
      context.stop(self)
  }

  def completeTask(yearMonth: String, managerActor: ActorRef, task: Task): Future[Int] = {
    taskService.updateTaskSuccess(task, s"Dados de $yearMonth removidos com sucesso.").flatMap { result =>
      managerActor ! ManagerActor.DataRemoved
      Future {result}
    }
  }

  def completeFailedTask(yearMonth: String, managerActor: ActorRef, task: Task): Future[Int] = {
    taskService.updateTaskSuccess(task, s"Dados de de $yearMonth não removidos totalmente, tente novamente.").flatMap { result =>
      managerActor ! ManagerActor.DataNotRemoved
      Future {
        result
      }
    }
  }

}
