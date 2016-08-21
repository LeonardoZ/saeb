package models.reader

import javax.inject.Inject

import akka.actor.{Actor, Props}
import akka.event.LoggingReceive
import models.entity.DataImport
import models.reader.ManagerActor.{DataImportDone, DataImportOrder, FileAlreadyImported, StartDataImport}
import play.api.Logger
import play.api.libs.concurrent.InjectedActorSupport

object ManagerActor {


  def props = Props[ManagerActor]

  case class DataImportOrder(path: String)

  case class StartDataImport(path: String)

  case class FileAlreadyImported(dataImport: DataImport)

  case object DataImportDone

}

class ManagerActor @Inject()(val dataImportFactory: DataImportActor.Factory,
                             val valuesFactory: ValuesManagerActor.Factory) extends Actor with InjectedActorSupport {

  override def preStart(): Unit = {
    Logger.debug("Starting SAEP Data Import...")

    // ex: self ! DataImportOrder("C:\\Users\\Leonardo\\Desktop\\Perfil_eleitorado\\perfil_eleitorado_1998.txt")
  }

  override def receive = LoggingReceive {

    case DataImportOrder(path) => {
      val dataImportActor = injectedChild(dataImportFactory(), "data-import-actor$" + System.nanoTime())
      dataImportActor ! DataImportActor.CheckFileAlreadyImported(self, path)
    }

    case StartDataImport(path) => {
      val valuesManagerActor = injectedChild(valuesFactory(), "values-manager-actor-$" + System.nanoTime())
      valuesManagerActor ! ValuesManagerActor.ReadValuesFromFile(self, path)
    }

    case FileAlreadyImported(dataImport) => {
      Logger.debug("Nothing to do.")

    }

    case DataImportDone => {
      context.stop(self)
    }
  }

}
