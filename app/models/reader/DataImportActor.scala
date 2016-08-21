package models.reader

import javax.inject.Inject

import akka.actor.{Actor, ActorRef}
import models.db.DataImportRepository
import models.service.ProfileFileParser
import play.api.Logger

object DataImportActor {

  trait Factory {
    def apply(): Actor
  }

  case class CheckFileAlreadyImported(ref: ActorRef, path: String)

  case class SaveNewImport(path: String)

}


class DataImportActor @Inject()(val dataImportRepository: DataImportRepository,
                                val profileFileParser: ProfileFileParser) extends Actor {

  import DataImportActor._
  import scala.concurrent.ExecutionContext.Implicits.global

  def receive: Receive = {
    case CheckFileAlreadyImported(ref, path) => {
      val dataImport = profileFileParser.parseFileData(path)
      dataImportRepository.getByFileNameAndYear(dataImport) map {
        case Some(dataImport) => {
          Logger.debug("Found: "+dataImport)
          ref ! ManagerActor.FileAlreadyImported(dataImport)
        }
        case None => {
          Logger.debug("File not imported yet: "+path)
          ref ! ManagerActor.StartDataImport(path)
        }
      }
    }
    case SaveNewImport(path) => {
      val dataImport = profileFileParser.parseFileData(path)
      dataImportRepository.insertReturningId(dataImport)
    }
  }
}
