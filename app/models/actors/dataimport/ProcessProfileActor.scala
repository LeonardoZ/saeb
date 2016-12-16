package models.actors.dataimport

import javax.inject.Inject

import akka.actor.{Actor, ActorRef}
import akka.util.Timeout
import models.db.{AgeGroupRepository, CityRepository, ProfileRepository, SchoolingRepository}
import models.entity._
import models.service.ProfileFileParser
import play.api.libs.concurrent.InjectedActorSupport

import scala.concurrent.{Await, ExecutionContext, Future}

object ProcessProfileActor {

  trait Factory {
    def apply(): Actor
  }

  case class StartFileReading(valuesManagerActor: ActorRef, path: String)

  case class ValuesLoaded(profiles: Stream[FullProfile])

  case class LoadValuesWithDb(profiles: Stream[FullProfile])

  case class ProfilePersisted()

  case class DataPersistComplete()

}

class ProcessProfileActor @Inject()(val dataImportFactory: DataImportActor.Factory,
                                    val profileRepository: ProfileRepository,
                                    val profileFileParser: ProfileFileParser,
                                    val cityRepository: CityRepository,
                                    val schoolingRepository: SchoolingRepository,
                                    val ageGroupRepository: AgeGroupRepository) extends Actor with InjectedActorSupport {

  import ProcessProfileActor._

  import scala.concurrent.ExecutionContext.Implicits.global
  import scala.concurrent.duration._

  implicit val timeout: Timeout = 2 minutes

  // for db item cache
  var cities = Map[(String, String, String), City]()
  var ages = Map[String, AgeGroup]()
  var schoolings = Map[String, Schooling]()

  // for future accumulation
  val fs = scala.collection.mutable.ListBuffer[Future[Any]]()

  var filePath: String = ""

  // parent
  var valuesManagerActor: ActorRef = null


  def receive: Receive = {
    case StartFileReading(ref, path) => {
      this.filePath = path
      this.valuesManagerActor = ref
      val profiles = profileFileParser.parseProfile(path)
      self ! LoadValuesWithDb(profiles)
    }

    case LoadValuesWithDb(profiles) => {
      val values: Future[(Seq[City], Seq[AgeGroup], Seq[Schooling])] = for {
        cs <- cityRepository.getAll
        as <- ageGroupRepository.getAll
        sc <- schoolingRepository.getAll
      } yield (cs, as, sc)

      values.map { vals =>
        cities = cities ++ (vals._1.map(c => ((c.code, c.name, c.country), c)))
        ages = ages ++ (vals._2.map(age => (age.group, age)))
        schoolings = schoolings ++ (vals._3.map(sc => (sc.level, sc)))
        self ! ValuesLoaded(profiles)
      }
    }

    case ValuesLoaded(profiles) => {

      implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global

      implicit val timeout: Timeout = 5.minutes

      // Stream[_] map doesn't map the elements
      profiles.grouped(5000).foreach { chunk =>
        val profiles: List[Profile] = chunk.toList.map(persistProfiles)
        fs += profileRepository.insertAll(profiles)
      }

      Await.ready(Future.sequence(fs), 5 minutes)
      self ! DataPersistComplete
    }

    case DataPersistComplete => {
      val importActor = injectedChild(dataImportFactory(), "data-import-actor$" + System.nanoTime())
      importActor ! DataImportActor.SaveNewImport(filePath)
      valuesManagerActor ! ValuesManagerActor.ProfilePesistenceDone
    }
  }

  def persistProfiles(profile: FullProfile): Profile = {

    val values: Option[(FullProfile, AgeGroup, Schooling, City)] = for {
      ageF <- ages.get(profile.ageGroup.group)
      schoolingF <- schoolings.get(profile.schooling.level)
      cityF <- cities.get((profile.city.code, profile.city.name, profile.city.country))
    } yield (profile, ageF, schoolingF, cityF)

    // map those values

    values match {
      case Some(vals: (FullProfile, AgeGroup, Schooling, City)) => convertToProfile(vals._1, vals._2, vals._3, vals._4)
      case None => (Profile())
    }
  }

  def convertToProfile(p: FullProfile, ageGroup: AgeGroup, schooling: Schooling, city: City): Profile = {
    Profile(yearOrMonth = p.yearOrMonth,
      electoralDistrict = p.electoralDistrict,
      sex = SexParser.convert(p.sex),
      quantityOfPeoples = p.quantityOfPeoples,
      cityId = city.id.get,
      schoolingId = schooling.id.get,
      ageGroupId = ageGroup.id.get)
  }


}