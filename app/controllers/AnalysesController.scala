package controllers

import javax.inject.{Inject, Named}

import akka.actor.ActorRef
import models.actors.analyses.AnalysesActor
import models.db._
import models.form.AnalysesForm
import play.api.data.Form
import play.api.data.Forms._
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.Controller

import scala.concurrent.{ExecutionContext, Future}


class AnalysesController @Inject()(@Named("analyses-actor") val analysesActor: ActorRef,
                                   val dataImportRepository: DataImportRepository,
                                   val messagesApi: MessagesApi)(implicit ec: ExecutionContext)
  extends Controller with I18nSupport with UserInfo {

  val analysesForm: Form[AnalysesForm] = Form {
    mapping(
      "yearMonth" -> nonEmptyText
    )(AnalysesForm.apply)(AnalysesForm.unapply)
  }


  def analysesPage = SecureRequest.async { implicit request =>
    dataImportRepository.getAll map { imports =>
        val years = importsToYearsForView(imports)
        Ok(views.html.analyses(analysesForm, years))
    }
  }


  def doAnalyses = SecureRequest.async { implicit request =>
    analysesForm.bindFromRequest.fold(
      error => Future {
        Redirect(routes.AnalysesController.analysesPage()).flashing(("error") ->
          s"Falha ao realizar análise.")
      },
      form => {
        analysesActor ! AnalysesActor.StartAnalyses(request.userEmail, form.yearMonth)
        Future {
          Redirect(routes.AnalysesController.analysesPage()).flashing(("success") ->
            s"Análises para ${form.yearMonth} sendo realizadas. O processo será concluído em breve.")
        }
      }
    )
  }



}
