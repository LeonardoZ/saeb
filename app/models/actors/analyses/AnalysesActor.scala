package models.actors.analyses

import java.util.UUID
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject

import akka.actor.Actor
import akka.event.LoggingReceive
import akka.util.Timeout
import models.db.{AgeGroupRankingRepository, ProfileRepository, SchoolingRankingRepository}
import models.entity.{City, Task}
import models.query.{ProfileWithCode, YearMonth}
import models.service.TaskService
import play.api.Logger
import play.api.libs.concurrent.InjectedActorSupport

object AnalysesActor {

  case class StartAnalyses(email: String, yearMonth: String)

  case class OnCitiesRetrieval(cities: Seq[City], yearMonth: String, task: Task)

  case class Ready(cities: Seq[City], yearMonth: String, task: Task)

}

class AnalysesActor @Inject()(val taskService: TaskService,
                              val ageGroupRankingRepository: AgeGroupRankingRepository,
                              val schoolingRankingRepository: SchoolingRankingRepository,
                              val citiesRetrieveFactory: CitiesRetrieveActor.Factory,
                              val profileRepository: ProfileRepository,
                              val cityAnalysesActorFactory: CityAnalysesActor.Factory)
  extends Actor with InjectedActorSupport {

  import AnalysesActor._

  import play.api.libs.concurrent.Execution.Implicits._

  var counter = new AtomicInteger(0)

  lazy val citiesActor = injectedChild(citiesRetrieveFactory(), s"cities-retrieve-actor")

  override def preStart(): Unit = {
    Logger.debug("Starting SAEP Data Analyses...")
  }

  override def receive = LoggingReceive {

    case StartAnalyses(email, yearMonth) => {
      taskService.createTask(s"Analise",s"Analisando cidade com dataset de $yearMonth", email).map {
        case Some(task) => citiesActor ! CitiesRetrieveActor.RetrieveCities(self, yearMonth, task)
        case _ => None
      }
    }

    case OnCitiesRetrieval(cities, yearMonth, task) => {
      implicit val time = akka.util.Timeout(3, TimeUnit.MINUTES)

      (for {
        schRemoved <- schoolingRankingRepository.remove(yearMonth)
        ageRemoved <- ageGroupRankingRepository.remove(yearMonth)
      } yield (schRemoved, ageRemoved)).map { k =>
        self ! Ready(cities, yearMonth, task)
      }
    }

    case Ready(cities , yearMonth, task) => {
      implicit val timeout = Timeout(10, TimeUnit.MINUTES)

      cities.groupBy(_.id).keySet.grouped(25) foreach { citiesIdBlock =>
        val cityAnalysesActor =
          injectedChild(cityAnalysesActorFactory(), s"city-analyses-${UUID.randomUUID().toString}")
        val ids = citiesIdBlock.map(_.get).toSeq
        val yearAndMonth = YearMonth.split(yearMonth)

        profileRepository.getProfilesByCitiesAndYear(yearAndMonth.year, yearAndMonth.month, ids) map { profiles =>
          val np = profiles.map {
            case (profile, city) =>
              ProfileWithCode(
                id = profile.id.get,
                yearOrMonth = profile.yearMonth,
                electoralDistrict = profile.electoralDistrict,
                sex = profile.sex,
                quantityOfPeoples = profile.quantityOfPeoples,
                cityId = profile.cityId,
                ageGroupId = profile.ageGroupId,
                schoolingId = profile.schoolingId,
                cityCode = city.code)
          }.toVector

          cityAnalysesActor ! CityAnalysesActor.CheckCities(self, yearMonth, np)
        }
      }
      taskService.updateTaskSuccess(task, s"Análises de ${yearMonth} realizadas com sucesso.")
    }
  }


}
