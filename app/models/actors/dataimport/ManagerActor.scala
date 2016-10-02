package models.actors.dataimport

import javax.inject.Inject

import akka.actor.{Actor, Props}
import akka.event.LoggingReceive
import models.actors.dataimport.ManagerActor.{DataImportDone, DataImportOrder, FileAlreadyImported, StartDataImport}
import models.entity.{DataImport, Task}
import models.service.TaskService
import play.api.Logger
import play.api.libs.concurrent.InjectedActorSupport

object ManagerActor {

  def props = Props[ManagerActor]

  case class DataImportOrder(path: String, userEmail: String)

  case class StartDataImport(task: Task, path: String)

  case class FileAlreadyImported(task: Task, dataImport: DataImport)

  case class DataImportDone(task: Task)

}

class ManagerActor @Inject()(val dataImportFactory: DataImportActor.Factory,
                             val valuesFactory: ValuesManagerActor.Factory,
                             val taskService: TaskService) extends Actor with InjectedActorSupport {

  override def preStart(): Unit = {
    Logger.debug("Starting SAEP Data Import...")
  }

  override def receive = LoggingReceive {
    case DataImportOrder(path, userEmail) => {

      val dataImportActor = injectedChild(dataImportFactory(), "data-import-actor$" + System.nanoTime())

      import scala.concurrent.ExecutionContext.Implicits.global
      val taskF = taskService.createTask(description = "Importar aquivo", "Importação sendo analisada", userEmail)
      taskF.map {
        case Some(task) => dataImportActor ! DataImportActor.CheckFileAlreadyImported(self, task, path)
        case None => None
      }
    }

    case StartDataImport(task, path) => {
      val valuesManagerActor = injectedChild(valuesFactory(), "values-manager-actor-$" + System.nanoTime())
      valuesManagerActor ! ValuesManagerActor.ReadValuesFromFile(self, task, path)
    }

    case FileAlreadyImported(task, dataImport) => {
      taskService.updateTaskSuccess(task, "Arquivo já importado no sistema.")
      Logger.debug("Nothing to do.")
    }

    case DataImportDone(task) => {

      taskService.updateTaskSuccess(task, "Arquivo importado com sucesso")
    }
  }



}
