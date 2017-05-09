package models.actors.scraper
import javax.inject.{Inject, Named}

import akka.actor.{Actor, ActorRef}
import akka.event.LoggingReceive
import akka.util.Timeout
import models.actors.dataimport.ManagerActor
import play.api.libs.concurrent.InjectedActorSupport

object ScraperActor {

  trait Factory {
    def apply(): Actor
  }
  case object DownloadAllAvailableDataSet

  case class DownloadAvailableDataSetOf(yearForDownload: YearForDownload, userEmail: String)

  case class YearDataSetDownloaded(downloadedYear: DownloadedYear, userEmail: String)

  case object AllDownloadsFinished

}


class ScraperActor @Inject()(@Named("manager-actor") val managerActor: ActorRef) extends Actor with InjectedActorSupport {

  import ScraperActor._
  import play.api.libs.concurrent.Execution.Implicits._

  import scala.concurrent.duration._

  implicit val timeout = Timeout(5 seconds) // needed for `?` below

  override def receive: Receive = LoggingReceive {

    // Client Calls
    case DownloadAvailableDataSetOf(yearForDownload, userEmail) => {

      DataSetDownloader.download(yearForDownload).map { downloadedYear =>
        self ! YearDataSetDownloaded(downloadedYear, userEmail)
      }
    }

      // Client calls
    case DownloadAllAvailableDataSet => {

    }

    case YearDataSetDownloaded(downloadedYear, userEmail) => {
      println("Downloaded: " + downloadedYear)
      managerActor ! ManagerActor.DataImportOrder(downloadedYear.pathToFileInTemp, userEmail)
    }

    case AllDownloadsFinished => {

    }
  }

}
