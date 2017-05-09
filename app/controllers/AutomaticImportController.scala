package controllers

import javax.inject.{Inject, Named}

import akka.actor.ActorRef
import models.actors.scraper.{ScraperActor, SiteScraper}
import models.db._
import models.form.AutomaticImportForm
import play.api.data.Form
import play.api.data.Forms._
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.Controller

import scala.concurrent.{ExecutionContext, Future}


class AutomaticImportController @Inject()(@Named("scraper-actor") val automaticActor: ActorRef,
                                          val dataImportRepository: DataImportRepository,
                                          val messagesApi: MessagesApi)(implicit ec: ExecutionContext)

  extends Controller with I18nSupport with UserInfo {

  val automaticForm: Form[AutomaticImportForm] = Form {
    mapping(
      "dataSetLabel" -> nonEmptyText
    )(AutomaticImportForm.apply)(AutomaticImportForm.unapply)
  }


  def automaticPage = SecureRequest.async { implicit request =>
    SiteScraper.scrapForYears map { yearsResult =>
      val years = yearsResult.map(_.yearInSite)
      Ok(views.html.automatic_import(automaticForm, years.zip(years)))
    }
  }


  def doSpecificAutomaticImport = SecureRequest.async { implicit request =>
    automaticForm.bindFromRequest.fold(
      error => Future {
        Redirect(routes.AutomaticImportController.automaticPage()).flashing(("error") ->
          s"Falha ao realizar importação.")
      },
      form => {
        SiteScraper.scrapForYears map { yearsResult =>
          yearsResult.find(y => form.dataSetLabel.equals(y.yearInSite)) match {
            case Some(yearResult) => {
              SiteScraper.scrapForContent(yearResult) map { yearForDownload =>
                automaticActor ! ScraperActor.DownloadAvailableDataSetOf(yearForDownload, request.userEmail)
                Redirect(routes.AutomaticImportController.automaticPage()).flashing(("success") ->
                  s"Importações para ${form.dataSetLabel} sendo realizadas. O processo será concluído em breve.")
              }
            }
            case None =>  Redirect(routes.AutomaticImportController.automaticPage()).flashing(("error") ->
              s"Conjunto de dados não encontrado.")

          }
        }
        Future {
          Redirect(routes.AutomaticImportController.automaticPage()).flashing(("success") ->
            s"Importações para ${form.dataSetLabel} sendo realizadas. O processo será concluído em breve.")
        }
      }
    )
  }

  def doAutomatic = SecureRequest.async { implicit request =>
    automaticActor ! ScraperActor.DownloadAllAvailableDataSet

    Future {
      Redirect(routes.AutomaticImportController.automaticPage()).flashing(("success") ->
        s"Importações sendo realizadas. O processo será concluído em breve.")
    }
  }






}
