package models.reader


import javax.inject.Inject

import akka.actor.{Actor, ActorRef}
import akka.event.LoggingReceive
import akka.pattern.{ask, pipe}
import akka.util.Timeout
import models.reader.AgeGroupPersistActor.AgeGroupPersistence
import models.reader.SchoolingsPersistActor.SchoolingsPersistence
import models.service.ProfileFileParser
import play.api.libs.concurrent.InjectedActorSupport

import scala.concurrent.duration._

object ValuesManagerActor {

  trait Factory {
    def apply(): Actor
  }


  case class StartProfileExtraction(file: String)

  case class ReadValuesFromFile(managerActor: ActorRef, file: String)

  case class CitiesPersistenceDone()

  case class AgeGroupPersistenceDone()

  case class SchoolingsPersistenceDone()

  object ProfilePesistenceDone

}

class ValuesManagerActor @Inject()(val profileFactory: ProfileWorkerActor.Factory,
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

  def checkIfEveryoneIsReady = {

    if (citiesReady && agesReady && schoolingsReady) {
      self ! StartProfileExtraction(filePath)
    }
  }

  def receive: Receive = LoggingReceive {
    case CitiesPersistenceDone => {
      citiesReady = true
      checkIfEveryoneIsReady
    }

    case SchoolingsPersistenceDone => {
      schoolingsReady = true
      checkIfEveryoneIsReady
    }

    case AgeGroupPersistenceDone => {
      agesReady = true
      checkIfEveryoneIsReady
    }

    case ReadValuesFromFile(managerActor, file) => {

      import scala.concurrent.ExecutionContext.Implicits.global

      implicit val timeout: Timeout = 2 minutes

      this.filePath = file
      this.managerActor = managerActor

      val values = profileFileParser.parseValues(file)
      val citiesActor = injectedChild(cityFactory(), "cities-persist-actor")
      val agesActor = injectedChild(ageFactory(), "ages-persist-actor")
      val schoolingsActor = injectedChild(schoolingFactory(), "schoolings-persist-actor")

      (citiesActor ? CitiesPersistActor.CitiesPersistence(self, values._1)).mapTo[CitiesPersistenceDone] pipeTo sender
      (agesActor ? AgeGroupPersistence(self, values._2)).mapTo[AgeGroupPersistenceDone] pipeTo sender
      (schoolingsActor ? SchoolingsPersistence(self, values._3)).mapTo[SchoolingsPersistenceDone] pipeTo sender
    }

    case StartProfileExtraction(file) => {
      val persistActor = injectedChild(profileFactory(), "profile-worker-actor$" + System.nanoTime())
      persistActor ! ProfileWorkerActor.StartFileReading(self, file)
    }

    case ProfilePesistenceDone => {
      managerActor ! ManagerActor.DataImportDone
    }
  }

  def readFromFile(path: String): Unit = {
    profileFileParser.parseValues(path)
  }

}
