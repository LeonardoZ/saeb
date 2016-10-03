package models.actors.analyses

import java.util.UUID
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject

import akka.actor.Actor
import akka.event.LoggingReceive
import akka.util.Timeout
import models.db.{AgeGroupRankingRepository, ProfileRepository, SchoolingRankingRepository}
import models.entity.City
import models.query.ProfileWithCode
import models.service.TaskService
import play.api.Logger
import play.api.libs.concurrent.InjectedActorSupport

import scala.concurrent.ExecutionContext

object AnalysesActor {

  case class StartAnalyses(email: String, yearMonth: String)

  case class OnCitiesRetrieval(cities: Seq[City], yearMonth: String)

  case class Ready(cities: Seq[City], yearMonth: String)

}

class AnalysesActor @Inject()(val taskService: TaskService,
                              val ageGroupRankingRepository: AgeGroupRankingRepository,
                              val schoolingRankingRepository: SchoolingRankingRepository,
                              val citiesRetrieveFactory: CitiesRetrieveActor.Factory,
                              val profileRepository: ProfileRepository,
                              val cityAnalysesActorFactory: CityAnalysesActor.Factory) extends Actor with InjectedActorSupport {


  import AnalysesActor._

  var counter = new AtomicInteger(0)

  lazy val citiesActor = injectedChild(citiesRetrieveFactory(), s"cities-retrieve-actor")

  override def preStart(): Unit = {
    Logger.debug("Starting SAEP Data Analyses...")
  }
  override def receive = LoggingReceive {

    case StartAnalyses(email, yearMonth) => {
      taskService.createTask(s"Analise","Analisando cidade com dataset de $yearMonth", email)
      citiesActor ! CitiesRetrieveActor.RetrieveCities(self, yearMonth)
    }

    case OnCitiesRetrieval(cities, yearMonth) => {
      implicit val time = akka.util.Timeout(3, TimeUnit.MINUTES)
      implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global

      (for {
        schRemoved <- schoolingRankingRepository.remove(yearMonth)
        ageRemoved <- ageGroupRankingRepository.remove(yearMonth)
      } yield (schRemoved, ageRemoved)).map { k =>
        self ! Ready(cities, yearMonth)
      }
    }

    case Ready(cities , yearMonth) => {
      implicit val timeout = Timeout(10, TimeUnit.MINUTES)
      implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global

      cities.groupBy(_.id).keySet.grouped(100) foreach { citiesIdBlock =>
        val cityAnalysesActor = injectedChild(cityAnalysesActorFactory(), s"city-analyses-"+UUID.randomUUID().toString)
        val ids = citiesIdBlock.map(_.get).toSeq
        profileRepository.getProfilesByCitiesAndYear2(yearMonth, ids) map { profiles =>
          val np: Vector[ProfileWithCode] = profiles.map((profileCity) => ProfileWithCode(
            id = profileCity._1.id.get,
            yearOrMonth = profileCity._1.yearOrMonth,
            electoralDistrict = profileCity._1.electoralDistrict,
            sex = profileCity._1.sex,
            quantityOfPeoples = profileCity._1.quantityOfPeoples,
            cityId = profileCity._1.cityId,
            ageGroupId = profileCity._1.ageGroupId,
            schoolingId = profileCity._1.schoolingId,
            cityCode = profileCity._2.code)).toVector

          cityAnalysesActor ! CityAnalysesActor.CheckCities(self, yearMonth, np)
        }
      }
    }


  }


}
