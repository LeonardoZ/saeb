package models.actors.dataimport

import javax.inject.Inject

import akka.actor.{Actor, ActorRef}
import models.db.SchoolingRepository
import models.entity.Schooling

import scala.concurrent.Future


object SchoolingsPersistActor {

  trait Factory {
    def apply(): Actor
  }

  case class SchoolingsPersistence(actorRef: ActorRef, schoolings: Set[Schooling])

}

class SchoolingsPersistActor @Inject()(val schoolingRepository: SchoolingRepository) extends Actor {

  import SchoolingsPersistActor._

  def receive: Receive = {
    case SchoolingsPersistence(ref, newSchoolings) => {
      import play.api.libs.concurrent.Execution.Implicits._
      val schoolingsToPersist: Future[Set[Schooling]] = schoolingRepository.getAll map { oldSchoolings =>
        newSchoolings.filter(ns => oldSchoolings.filter(ns.level == _.level).isEmpty)
      }

      val f = schoolingsToPersist.map(cs => {
        if (cs.isEmpty)
          Future.successful()
        else
          schoolingRepository.insertAll(cs)
      })

      f.map { futureProcess =>
        ref ! ValuesManagerActor.SchoolingsPersistenceDone(self)
      }
    }
  }
}
