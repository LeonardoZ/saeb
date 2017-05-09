package models.actors
import com.google.inject.AbstractModule
import models.actors.analyses._
import models.actors.dataimport._
import models.actors.scraper.ScraperActor
import play.api.libs.concurrent.AkkaGuiceSupport

class ActorsModule extends AbstractModule with AkkaGuiceSupport {

  def configure(): Unit = {
    bindActor[ManagerActor]("manager-actor")
    bindActor[AnalysesActor]("analyses-actor")
    bindActor[ScraperActor]("scraper-actor")

    // import data actors
    bindActorFactory[ValuesManagerActor, ValuesManagerActor.Factory]
    bindActorFactory[CitiesPersistActor, CitiesPersistActor.Factory]
    bindActorFactory[AgeGroupPersistActor, AgeGroupPersistActor.Factory]
    bindActorFactory[SchoolingsPersistActor, SchoolingsPersistActor.Factory]
    bindActorFactory[ProcessProfileActor, ProcessProfileActor.Factory]
    bindActorFactory[DataImportActor, DataImportActor.Factory]
    bindActorFactory[DataRemovalActor, DataRemovalActor.Factory]

    // analyses actors
    bindActorFactory[AgeGroupAnalysesActor, AgeGroupAnalysesActor.Factory]
    bindActorFactory[CitiesRetrieveActor, CitiesRetrieveActor.Factory]
    bindActorFactory[CityAnalysesActor, CityAnalysesActor.Factory]
    bindActorFactory[SchoolingAnalysesActor, SchoolingAnalysesActor.Factory]

    // scraper actors

  }
}
