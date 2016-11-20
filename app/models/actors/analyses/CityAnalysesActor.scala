package models.actors.analyses

import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject

import akka.actor.{Actor, ActorRef}
import akka.event.LoggingReceive
import akka.routing.{ActorRefRoutee, RoundRobinRoutingLogic, Router}
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
  var counter: AtomicInteger = new AtomicInteger(0)

  var ageGroupRouter = {
    val routees = Vector.fill(Runtime.getRuntime.availableProcessors()) {
      val r = injectedChild(ageGroupFactory(), s"age-group-analyses-" + System.nanoTime())
      context watch r
      ActorRefRoutee(r)
    }
    Router(RoundRobinRoutingLogic(), routees)
  }

  var schoolingRouter = {
    val routees = Vector.fill(Runtime.getRuntime.availableProcessors()) {
      val r =  injectedChild(schoolingFactory(), s"schooling-analyses-" + System.nanoTime())
      context watch r
      ActorRefRoutee(r)
    }
    Router(RoundRobinRoutingLogic(), routees)
  }


  def receive: Receive = LoggingReceive {
    case CheckCities(father, yearMonth, profilesWithCode: Vector[ProfileWithCode]) => {
      ageGroupRouter.route(AgeGroupAnalysesActor.AgeGroupMultiAnalyses(self, yearMonth, profilesWithCode), self)
      schoolingRouter.route(SchoolingAnalysesActor.SchoolingMultiAnalyses(self, yearMonth, profilesWithCode), self)
    }
  }

}
