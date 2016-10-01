package models.actors.analyses

import javax.inject.Inject

import akka.actor.{Actor, ActorRef}
import akka.event.LoggingReceive
import models.actors.analyses.CitiesRetrieveActor.RetrieveCities
import models.db.CityRepository
import play.api.libs.concurrent.InjectedActorSupport

import scala.concurrent.ExecutionContext

object CitiesRetrieveActor {

  trait Factory {
    def apply(): Actor
  }

  case class RetrieveCities(analysesActorRef: ActorRef, yearMonth: String)

}

class CitiesRetrieveActor @Inject()(val cityRepository: CityRepository) extends Actor with InjectedActorSupport {

  implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global

  override def receive: Receive = LoggingReceive {
    case RetrieveCities(analysesActorRef, yearMonth) =>
      cityRepository.getAll().map { cities =>
        analysesActorRef ! AnalysesActor.OnCitiesRetrieval(cities, yearMonth)
      }

  }
}
