package models.actors.analyses

import javax.inject.Inject

import akka.actor.{Actor, ActorRef}
import akka.event.LoggingReceive
import models.actors.analyses.CitiesRetrieveActor.RetrieveCities
import models.db.CityRepository
import models.entity.Task
import play.api.libs.concurrent.InjectedActorSupport

object CitiesRetrieveActor {

  trait Factory {
    def apply(): Actor
  }

  case class RetrieveCities(analysesActorRef: ActorRef, yearMonth: String, task: Task)

  case class CountCities(analysesActorRef: ActorRef, yearMonth: String, task: Task)

  case class RetrieveCitiesPaginated(analysesActorRef: ActorRef, yearMonth: String, task: Task, limit: Int, offset: Int)

}

class CitiesRetrieveActor @Inject()(val cityRepository: CityRepository) extends Actor with InjectedActorSupport {

  import play.api.libs.concurrent.Execution.Implicits._

  override def receive: Receive = LoggingReceive {
    case RetrieveCities(analysesActorRef, yearMonth, task) =>
      cityRepository.getAll().map { cities =>
        analysesActorRef ! AnalysesActor.OnCitiesRetrieval(cities, yearMonth, task)
        context.stop(self)
      }
  }
}
