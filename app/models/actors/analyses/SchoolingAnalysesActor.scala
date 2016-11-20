package models.actors.analyses

import javax.inject.Inject

import akka.actor.{Actor, ActorRef}
import akka.event.LoggingReceive
import akka.util.Timeout
import models.db.SchoolingRankingRepository
import models.entity.SchoolingRanking
import models.query.ProfileWithCode
import play.api.libs.concurrent.InjectedActorSupport

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._


object SchoolingAnalysesActor {

  trait Factory {
    def apply(): Actor
  }

  case class SchoolingMultiAnalyses(cityAnalysesActor: ActorRef, yearMonth: String, profiles: Vector[ProfileWithCode])

}

class SchoolingAnalysesActor @Inject()(val rankingRepository: SchoolingRankingRepository) extends Actor with InjectedActorSupport {

  implicit val timeout: Timeout = 2 minutes
  implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global
  import SchoolingAnalysesActor._


  override def receive: Receive = LoggingReceive {
    case SchoolingMultiAnalyses(cityAnalysesActor, yearMonth, profiles: Vector[ProfileWithCode]) => {
      val allSchoolingAnalysesInChunk: Seq[SchoolingRanking] = profiles.groupBy(_.cityCode).flatMap {
        case (code, profiles) => profilesAnalyze(code, yearMonth, profiles)
      }.toSeq
      rankingRepository.insertAll(allSchoolingAnalysesInChunk)
    }
  }

  def profilesAnalyze(code: String, year: String, profiles: Vector[ProfileWithCode]): Seq[SchoolingRanking] = {
    val total = profiles.map(_.quantityOfPeoples).sum

    profiles.groupBy(_.schoolingId).map {
      case (schoolingId, profiles) => (schoolingId, profiles.map(_.quantityOfPeoples).sum)
    }.map {
      case (schoolingId, peoples) =>
        SchoolingRanking(
          cityCode = code,
          yearMonth = year,
          schoolingId = schoolingId,
          peoples = peoples,
          percentOfTotal = percentageOf(peoples, total),
          total = total)
    }.toSeq
  }

}
