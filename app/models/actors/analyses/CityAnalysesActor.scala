package models.actors.analyses

import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject

import akka.actor.{Actor, ActorRef}
import akka.event.LoggingReceive
import akka.util.Timeout
import models.db.ProfileRepository
import models.entity.Profile
import models.query.ProfileWithCode
import play.api.libs.concurrent.InjectedActorSupport

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._


object CityAnalysesActor {

  trait Factory {
    def apply(): Actor
  }

  case class CheckCities(father: ActorRef, yearMonth: String, profilesWithCode: Vector[ProfileWithCode])
  case class CheckCities2(father: ActorRef, yearMonth: String, profilesWithCode: Seq[Profile])

}

class CityAnalysesActor @Inject()(val schoolingFactory: SchoolingAnalysesActor.Factory,
                                  val ageGroupFactory: AgeGroupAnalysesActor.Factory,
                                  val profileRepository: ProfileRepository) extends Actor with InjectedActorSupport {

  implicit val timeout: Timeout = 2 minutes
  implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global

  import CityAnalysesActor._

  val ageAnalysesActorS = injectedChild(ageGroupFactory(), s"age-group-analyses-single")
  val schoolingAnalysesActorS = injectedChild(schoolingFactory(), s"schooling-analyses-single")
  var counter : AtomicInteger = new AtomicInteger(0)

  def receive: Receive = LoggingReceive {

    case CheckCities(father, yearMonth, profilesWithCode: Vector[ProfileWithCode]) => {
      println("===== Size "+ profilesWithCode.size)
      val ageAnalysesActor = injectedChild(ageGroupFactory(), s"age-group-analyses-" + System.nanoTime())
      val schoolingAnalysesActor = injectedChild(schoolingFactory(), s"schooling-analyses-" + System.nanoTime())
      schoolingAnalysesActor ! SchoolingAnalysesActor.SchoolingMultiAnalyses(self, yearMonth, profilesWithCode)
      ageAnalysesActor ! AgeGroupAnalysesActor.AgeGroupMultiAnalyses(self, yearMonth, profilesWithCode)
    }
//
//    case CheckCities2(father, yearMonth, profilesWithCode: Seq[ProfileWithCode]) => {
//      println("===== Size "+ profilesWithCode.size)
//      val ageAnalysesActor = injectedChild(ageGroupFactory(), s"age-group-analyses-" + System.nanoTime())
//      val schoolingAnalysesActor = injectedChild(schoolingFactory(), s"schooling-analyses-" + System.nanoTime())
//      schoolingAnalysesActor ! SchoolingAnalysesActor.SchoolingMultiAnalyses(self, yearMonth, profilesWithCode)
//      ageAnalysesActor ! AgeGroupAnalysesActor.AgeGroupMultiAnalyses2(self, yearMonth, profilesWithCode)
//    }

  }

}
