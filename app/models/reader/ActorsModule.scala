package models.reader
import com.google.inject.AbstractModule
import play.api.libs.concurrent.AkkaGuiceSupport
/**
  * Created by Leonardo on 15/08/2016.
  */
class ActorsModule extends AbstractModule with AkkaGuiceSupport {

  def configure(): Unit = {
    bindActor[ManagerActor]("manager-actor")
    bindActorFactory[ValuesManagerActor, ValuesManagerActor.Factory]
    bindActorFactory[CitiesPersistActor, CitiesPersistActor.Factory]
    bindActorFactory[AgeGroupPersistActor, AgeGroupPersistActor.Factory]
    bindActorFactory[SchoolingsPersistActor, SchoolingsPersistActor.Factory]
    bindActorFactory[ProfileWorkerActor, ProfileWorkerActor.Factory]
    bindActorFactory[DataImportActor, DataImportActor.Factory]
  }
}
