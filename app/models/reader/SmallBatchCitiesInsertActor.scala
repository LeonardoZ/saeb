package models.reader

import java.util.concurrent.Executors
import javax.inject.Inject

import akka.actor.Actor
import akka.event.LoggingReceive
import akka.util.Timeout
import models.db.CityRepository
import models.entity.City
import play.api.Logger
import play.api.libs.concurrent.InjectedActorSupport

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

object SmallBatchCitiesInsertActor {
  trait Factory {
    def apply(): Actor
  }
}

class SmallBatchCitiesInsertActor @Inject()(val cityRepository: CityRepository)(implicit ex: ExecutionContext) extends Actor with InjectedActorSupport {

  implicit val exec = ExecutionContext.fromExecutor(Executors.newCachedThreadPool)

  def receive: Receive = LoggingReceive {
    case xs: Set[City] => {
      implicit val timeout: Timeout = 2.minutes
      cityRepository.insertAll(xs).recover({case ex => Logger.debug("Error "+ex.getMessage)})
    }
  }

}
