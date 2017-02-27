package models.actors.dataimport

import javax.inject.Inject

import akka.actor.{Actor, ActorRef}
import akka.event.LoggingReceive
import models.db.AgeGroupRepository
import models.entity.AgeGroup

import scala.concurrent.{ExecutionContext, Future}

object AgeGroupPersistActor {

  trait Factory {
    def apply(): Actor
  }

  case class AgeGroupPersistence(actorRef: ActorRef, ageGroups: Set[AgeGroup])

}

class AgeGroupPersistActor @Inject()(val ageGroupRepository: AgeGroupRepository) extends Actor {

  import AgeGroupPersistActor._

  import scala.concurrent.ExecutionContext.Implicits.global

  def receive: Receive = LoggingReceive {
    case AgeGroupPersistence(ref, newAgeGroups) => {
      implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global
      val ageGroupsToPersist: Future[Set[AgeGroup]] = ageGroupRepository.getAll map { oldAgeGroups =>
        newAgeGroups.filter(nag => oldAgeGroups.filter(nag.group == _.group).isEmpty)
      }
      val f = ageGroupsToPersist.flatMap { cs =>
        if (cs.isEmpty)
          Future.successful()
        else
          ageGroupRepository.insertAll(cs)
      }
      f.map { futureProcess =>
        ref ! ValuesManagerActor.AgeGroupPersistenceDone(self)
      }

    }
  }
}
