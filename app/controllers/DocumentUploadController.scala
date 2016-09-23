package controllers

import javax.inject.Inject

import akka.actor.ActorRef
import com.google.inject.name.Named
import controllers.security.SecureRequest
import models.db._
import models.reader.ManagerActor
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.Controller

import scala.concurrent.{ExecutionContext, Future}

class DocumentUploadController @Inject()(@Named("manager-actor") val managerActor: ActorRef,
                                          val dataImportRepository: DataImportRepository,
                                          val userRepo: UserRepository,
                                          val messagesApi: MessagesApi)(implicit ec: ExecutionContext)
  extends Controller with I18nSupport {

  def uploadPage = SecureRequest.async { implicit request =>
    Future(Ok(views.html.file_upload()).flashing())
  }

  def doUpload = SecureRequest(parse.multipartFormData) { request =>
    request.body.file("document").map { document =>
      import java.io.File
      val filename = document.filename
      val contentType = document.contentType
      val file = new File(s"/tmp/document/$filename")
//      document.ref.moveTo(new File(s"/tmp/document/$filename"))
      managerActor ! ManagerActor.DataImportOrder(document.ref.file.getPath)
      Ok("File uploaded")
      Redirect(routes.DocumentUploadController.uploadPage()).flashing(
        "success" -> "File being processed")
    }.getOrElse {
      Redirect(routes.DocumentUploadController.uploadPage()).flashing(
        "error" -> "Missing file")
    }
  }

}
