package models.reader

import javax.inject.Inject

import akka.actor.{Actor, ActorRef}
import akka.event.LoggingReceive
import akka.util.Timeout
import models.db.CityRepository
import models.entity.City
import play.api.libs.concurrent.InjectedActorSupport

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

object CitiesPersistActor {

  trait Factory {
    def apply(): Actor
  }

  case class CitiesPersistence(actorRef: ActorRef, cities: Set[City])

  case object BatchInsertDone

  case object CitySaved

}

class CitiesPersistActor @Inject()(val smallBatchFactory: SmallBatchCitiesInsertActor.Factory,
                                   val cityRepository: CityRepository) extends Actor with InjectedActorSupport {

  import CitiesPersistActor._

  implicit val timeout: Timeout = 2 minutes

  def receive: Receive = LoggingReceive {

    case CitiesPersistence(actorRef, newCities) =>
      implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global
      val citiesToPersist: Future[Set[City]] = cityRepository.getAll map { oldCities =>
        newCities.filter(nc => oldCities.filter(nc.contentEqual(_)).isEmpty)
      }
      val f = citiesToPersist.flatMap { cs =>
        if (cs.isEmpty)
          Future.successful()
        else
          cityRepository.insertAll(cs)
      }
      f.map { futureProcess => {
        actorRef ! ValuesManagerActor.CitiesPersistenceDone
        context.stop(self)
      }}
  }

}
