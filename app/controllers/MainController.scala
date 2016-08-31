package controllers

import javax.inject.Inject

import controllers.security.SecureRequest
import models.db._
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.Controller

import scala.concurrent.{ExecutionContext, Future}

class MainController @Inject()(val dataImportRepository: DataImportRepository,
                               val cityRepository: CityRepository,
                               val schoolingRepository: SchoolingRepository,
                               val ageGroupRepository: AgeGroupRepository,
                               val userRepo: UserRepository,
                               val messagesApi: MessagesApi)(implicit ec: ExecutionContext)
  extends Controller with I18nSupport {

  def index = SecureRequest.async {
    val values = for {
      groups <- ageGroupRepository.count
      schoolings <- schoolingRepository.count
      cities <- cityRepository.getBrazilianCities
      brStates <- cityRepository.getBrazilianStates
      dataImports <- dataImportRepository.getAll
    } yield (groups, schoolings, cities, brStates, dataImports)

    val newValues: Future[(Int, Int, Int, Int, String, String)] = values.flatMap(vals => {
      val imports = vals._5
      val firstAndLastYears = if (imports.size > 0)
        (imports.head.fileYear, imports.last.fileYear)
      else
        ("Nada encontrado.", "Nada encontrado")
      Future((vals._1, vals._2, vals._3, vals._4, firstAndLastYears._1, firstAndLastYears._2))
    })

    newValues map { vals =>
      Ok(views.html.admin(vals._1, vals._2, vals._3, vals._4, vals._5, vals._6))
    }
  }

}
