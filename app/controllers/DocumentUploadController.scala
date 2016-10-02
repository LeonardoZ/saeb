package controllers

import java.io.File
import java.util.UUID
import javax.inject.Inject

import akka.actor.ActorRef
import com.google.common.io.Files
import com.google.inject.name.Named
import controllers.security.SecureRequest
import models.actors.dataimport.ManagerActor
import models.db._
import models.service.TaskService
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.libs.json.Json
import play.api.mvc.Controller

import scala.concurrent.{ExecutionContext, Future}

class DocumentUploadController @Inject()(@Named("manager-actor") val managerActor: ActorRef,
                                         val dataImportRepository: DataImportRepository,
                                         val taskService: TaskService,
                                         val userRepo: UserRepository,
                                         val messagesApi: MessagesApi)(implicit ec: ExecutionContext)
  extends Controller with I18nSupport {

  case class FileUploadReturn(error: String, name: String, size: Long)

  implicit val peoplesInAgeGroupSchoolingFormat = Json.format[FileUploadReturn]
  implicit val yearCityCodesReads = Json.reads[FileUploadReturn]

  def uploadPage = SecureRequest.async { implicit request =>
    Future(Ok(views.html.file_upload()).flashing())
  }

  def doUpload = SecureRequest(parse.multipartFormData) { implicit request =>
    request.body.file("document").map { document =>
      val filename = UUID.randomUUID().toString
      val file = document.ref
      val moved = file.moveTo(new File("/tmp/" + filename))
      val filePath = moved.getAbsolutePath
      managerActor ! ManagerActor.DataImportOrder(filePath, request.userEmail)
      Ok(Json.obj("files" -> FileUploadReturn("", name = filename, size = moved.length)))
    }.getOrElse {
      Ok(Json.obj("files" -> FileUploadReturn("", name = "No file", size = 0)))
    }
  }

}
