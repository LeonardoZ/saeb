package models.reader

import javax.inject.Inject

import akka.actor.{Actor, ActorRef}
import models.db.DataImportRepository
import models.entity.Task
import models.service.ProfileFileParser
import play.api.Logger

object DataImportActor {

  trait Factory {
    def apply(): Actor
  }

  case class CheckFileAlreadyImported(ref: ActorRef, task: Task, path: String)

  case class SaveNewImport(path: String)

}


class DataImportActor @Inject()(val dataImportRepository: DataImportRepository,
                                val profileFileParser: ProfileFileParser) extends Actor {

  import DataImportActor._
  import scala.concurrent.ExecutionContext.Implicits.global

  def receive: Receive = {
    case CheckFileAlreadyImported(ref, task, path) => {
      val dataImport = profileFileParser.parseFileData(path)
      dataImportRepository.getByYearMonth(dataImport) map {
        case Some(dataImport) => {
          Logger.debug("Found: "+dataImport)
          ref ! ManagerActor.FileAlreadyImported(task, dataImport)
        }
        case None => {
          Logger.debug("File not imported yet: "+path)
          ref ! ManagerActor.StartDataImport(task, path)
        }
      }
    }

    case SaveNewImport(path) => {
      val dataImport = profileFileParser.parseFileData(path)
      dataImportRepository.insertReturningId(dataImport)
    }
  }
}
