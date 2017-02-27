package models.actors.dataimport

import javax.inject.Inject

import akka.actor.{Actor, ActorRef}
import akka.event.LoggingReceive
import akka.pattern.{ask, pipe}
import akka.util.Timeout
import models.actors.dataimport.AgeGroupPersistActor.AgeGroupPersistence
import models.actors.dataimport.SchoolingsPersistActor.SchoolingsPersistence
import models.entity.Task
import models.service.ProfileFileParser
import play.api.libs.concurrent.InjectedActorSupport

import scala.concurrent.duration._

object ValuesManagerActor {

  trait Factory {
    def apply(): Actor
  }


  case class StartProfileExtraction(file: String)

  case class ReadValuesFromFile(managerActor: ActorRef, task: Task, file: String)

  case class CitiesPersistenceDone(actorRef: ActorRef)

  case class AgeGroupPersistenceDone(actorRef: ActorRef)

  case class SchoolingsPersistenceDone(actorRef: ActorRef)

  object ProfilePesistenceDone

}

class ValuesManagerActor @Inject()(val profileFactory: ProcessProfileActor.Factory,
                                   val cityFactory: CitiesPersistActor.Factory,
                                   val ageFactory: AgeGroupPersistActor.Factory,
                                   val schoolingFactory: SchoolingsPersistActor.Factory,
                                   val profileFileParser: ProfileFileParser) extends Actor with InjectedActorSupport {

  import ValuesManagerActor._


  var citiesReady: Boolean = false
  var agesReady: Boolean = false
  var schoolingsReady: Boolean = false
  var filePath: String = ""
  var managerActor: ActorRef = null
  var task: Task = null


  def checkIfEveryoneIsReady = {
    if (citiesReady && agesReady && schoolingsReady) {
      self ! StartProfileExtraction(filePath)
    }
  }

  def receive: Receive = LoggingReceive {
    case CitiesPersistenceDone(actorRef) => {
      citiesReady = true
      checkIfEveryoneIsReady
      context.stop(actorRef)
    }

    case SchoolingsPersistenceDone(actorRef) => {
      schoolingsReady = true
      checkIfEveryoneIsReady
      context.stop(actorRef)
    }

    case AgeGroupPersistenceDone(actorRef) => {
      agesReady = true
      checkIfEveryoneIsReady
      context.stop(actorRef)
    }

    case ReadValuesFromFile(managerActor, task, file) => {

      import scala.concurrent.ExecutionContext.Implicits.global

      implicit val timeout: Timeout = 2 minutes

      this.filePath = file
      this.managerActor = managerActor
      this.task = task
      val values = profileFileParser.parseValues(file)
      val citiesActor = injectedChild(cityFactory(), "cities-persist-actor$" + task.userId)
      val agesActor = injectedChild(ageFactory(), "ages-persist-actor$" + task.userId)
      val schoolingsActor = injectedChild(schoolingFactory(), "schoolings-persist-actor$" + task.userId)

      (citiesActor ? CitiesPersistActor.CitiesPersistence(self, values._1))
          .mapTo[CitiesPersistenceDone] pipeTo self
      (agesActor ? AgeGroupPersistence(self, values._2))
          .mapTo[AgeGroupPersistenceDone] pipeTo self
      (schoolingsActor ? SchoolingsPersistence(self, values._3))
          .mapTo[SchoolingsPersistenceDone] pipeTo self
    }

    case StartProfileExtraction(file) => {
      val persistActor = injectedChild(profileFactory(), "profile-worker-actor$")
      persistActor ! ProcessProfileActor.StartFileReading(self, file, task.userId)
    }

    case ProfilePesistenceDone => {
      managerActor ! ManagerActor.DataImportDone(task)
      context.stop(self)

    }
  }

  def readFromFile(path: String): Unit = {
    profileFileParser.parseValues(path)
  }

}
