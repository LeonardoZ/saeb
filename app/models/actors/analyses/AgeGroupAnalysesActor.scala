package models.actors.analyses

import javax.inject.Inject

import akka.actor.{Actor, ActorRef}
import akka.event.LoggingReceive
import akka.util.Timeout
import models.db.AgeGroupRankingRepository
import models.entity.{AgeGroupRanking, Profile}
import models.query.ProfileWithCode
import play.api.libs.concurrent.InjectedActorSupport

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._


object AgeGroupAnalysesActor {

  trait Factory {
    def apply(): Actor
  }

  case class AgeGroupMultiAnalyses(cityAnalysesActor: ActorRef, yearMonth: String, profiles: Vector[ProfileWithCode])

  case class AgeGroupMultiAnalyses2(cityAnalysesActor: ActorRef, yearMonth: String, profiles: Seq[Profile])

}

class AgeGroupAnalysesActor @Inject()(val rankingRepository: AgeGroupRankingRepository)
  extends Actor with InjectedActorSupport {

  implicit val timeout: Timeout = 2 minutes
  implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global

  import AgeGroupAnalysesActor._


  override def receive: Receive = LoggingReceive {
    case AgeGroupMultiAnalyses(cityAnalysesActor, yearMonth, profiles: Vector[ProfileWithCode]) => {
      val allProfilesInChunk: Seq[AgeGroupRanking] = profiles.groupBy(p => p.cityCode).flatMap {
        case (code, profiles) => profilesAnalyze(code, yearMonth, profiles)
      }.toSeq
      rankingRepository.insertAll(allProfilesInChunk)
    }

//    case AgeGroupMultiAnalyses2(cityAnalysesActor, yearMonth, profiles: Vector[ProfileWithCode]) => {
//      val allProfilesInChunk: Seq[AgeGroupRanking] = profiles.groupBy(p => p.cityCode).flatMap {
//        case (code, profiles) => profilesAnalyze(code, yearMonth, profiles)
//      }.toSeq
//      rankingRepository.insertAll(allProfilesInChunk)
//    }
  }

  def profilesAnalyze(code: String, year: String, profiles: Vector[ProfileWithCode]): Seq[AgeGroupRanking] = {
    val total = profiles.map(_.quantityOfPeoples).sum

    profiles.groupBy(_.ageGroupId).map {
      case (ageGroupId, profiles) => (ageGroupId, profiles.map(_.quantityOfPeoples).sum)
    }.map {
      case (ageGroupId, peoples) =>
        AgeGroupRanking(
          cityCode = code,
          yearMonth = year,
          ageGroupId = ageGroupId,
          peoples = peoples,
          percentOfTotal = percentageOf(peoples, total),
          total = total)
    }.toSeq
  }


}