package controllers

import javax.inject.Inject

import models.db._
import models.entity.{DataImport, Task}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, Controller}

import scala.concurrent.{ExecutionContext, Future}

class MainController @Inject()(val dataImportRepository: DataImportRepository,
                               val cityRepository: CityRepository,
                               val taskRepository: TaskRepository,
                               val schoolingRepository: SchoolingRepository,
                               val ageGroupRepository: AgeGroupRepository,
                               val userRepo: UserRepository,
                               val messagesApi: MessagesApi)(implicit ec: ExecutionContext)

  extends Controller with I18nSupport with UserInfo {

  def index = SecureRequest.async { implicit request: AuthenticatedRequest[AnyContent] =>
    val values = for {
      groups <- ageGroupRepository.count
      schoolings <- schoolingRepository.count
      cities <- cityRepository.getBrazilianCities
      brStates <- cityRepository.getBrazilianStates
      dataImports <- dataImportRepository.getAll
      tasksUser <- taskRepository.getAll
    } yield (groups, schoolings, cities, brStates, dataImports, tasksUser)

    val newValues: Future[(Int, Int, Int, Int, String, String, Seq[DataImport], Seq[(Task, String)])] = values.flatMap(vals => {
      val imports = vals._5
      val firstAndLastYears = if (imports.size > 0)
        (imports.head.fileYear, imports.last.fileYear)
      else
        ("Nada encontrado.", "Nada encontrado")
      Future {
        (vals._1, vals._2, vals._3, vals._4, firstAndLastYears._1, firstAndLastYears._2, vals._5.sortBy(_.fileYear), (vals._6.map{
          case (task,user) => (task, user.email)
        }))
      }
    })

    newValues map { vals =>
      Ok(views.html.admin(vals._1, vals._2, vals._3, vals._4, vals._5, vals._6, vals._7, vals._8))
    }
  }



}
