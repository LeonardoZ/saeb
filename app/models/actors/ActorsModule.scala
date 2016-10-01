package models.actors
import com.google.inject.AbstractModule
import models.actors.analyses._
import models.actors.dataimport._
import play.api.libs.concurrent.AkkaGuiceSupport
/**
  * Created by Leonardo on 15/08/2016.
  */
class ActorsModule extends AbstractModule with AkkaGuiceSupport {

  def configure(): Unit = {
    bindActor[ManagerActor]("manager-actor")
    bindActor[AnalysesActor]("analyses-actor")
    // import data actors
    bindActorFactory[ValuesManagerActor, ValuesManagerActor.Factory]
    bindActorFactory[CitiesPersistActor, CitiesPersistActor.Factory]
    bindActorFactory[AgeGroupPersistActor, AgeGroupPersistActor.Factory]
    bindActorFactory[SchoolingsPersistActor, SchoolingsPersistActor.Factory]
    bindActorFactory[ProfileWorkerActor, ProfileWorkerActor.Factory]
    bindActorFactory[DataImportActor, DataImportActor.Factory]


    // analyses actors
    bindActorFactory[AgeGroupAnalysesActor, AgeGroupAnalysesActor.Factory]
    bindActorFactory[CitiesRetrieveActor, CitiesRetrieveActor.Factory]
    bindActorFactory[CityAnalysesActor, CityAnalysesActor.Factory]
    bindActorFactory[SchoolingAnalysesActor, SchoolingAnalysesActor.Factory]

  }
}
