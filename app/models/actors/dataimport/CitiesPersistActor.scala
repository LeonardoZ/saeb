package models.actors.dataimport

import javax.inject.Inject

import akka.actor.{Actor, ActorRef}
import akka.event.LoggingReceive
import akka.util.Timeout
import models.db.CityRepository
import models.entity.{City, SimpleCity}
import play.api.libs.concurrent.InjectedActorSupport

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._

object CitiesPersistActor {

  trait Factory {
    def apply(): Actor
  }

  case class CitiesPersistence(actorRef: ActorRef, cities: Set[SimpleCity])

  case object BatchInsertDone

  case object CitySaved

}

class CitiesPersistActor @Inject()(val cityRepository: CityRepository) extends Actor with InjectedActorSupport {

  import CitiesPersistActor._

  implicit val timeout: Timeout = 2 minutes


  def receive: Receive = LoggingReceive {

    case CitiesPersistence(actorRef, newCities) =>
      implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global
      val citiesToPersist = cityRepository.getAll map { oldCities =>
        val checkedCities = newCities.map { city =>
          oldCities.find(nc => nc.code.equals(city.id)) match {
            case Some(cityInBD) =>
              if (!cityInBD.names.contains(city.name))
                cityInBD.copy(names = (cityInBD.names :+ city.name))
              else
                cityInBD
            case None =>
              City(code = city.id, state = city.state, country = city.country, names = List() :+ city.name)
          }
        }
        val partitioned: (Set[City], Set[City]) = checkedCities.partition(_.id.isDefined)
//        partitioned._1.map(cityRepository.update(_)).map { c1 =>
//          cityRepository.insertAll(partitioned._2).onComplete { c2 =>
//            println("Acabando!")
//            actorRef ! ValuesManagerActor.CitiesPersistenceDone
//            context.stop(self)
//          }
//        }
        Future.sequence(partitioned._1.map(cityRepository.update(_))).onComplete { c1 =>
          cityRepository.insertAll(partitioned._2).onComplete { c2 =>
            println("Acabando!")
            actorRef ! ValuesManagerActor.CitiesPersistenceDone
            context.stop(self)
          }
        }
      }
  }
}
