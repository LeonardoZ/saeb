package controllers

import javax.inject.Inject

import akka.actor.ActorRef
import com.google.inject.name.Named
import controllers.security.SecureRequest
import models.db._
import models.reader.ManagerActor
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

  def uploadPage = SecureRequest.async { implicit request =>
    Future(Ok(views.html.file_upload()).flashing())
  }
  var str = """
  {
    files:
    [
    {
      url: "http://url.to/file/or/page",
      thumbnail_url: "http://url.to/thumnail.jpg ",
      name: "thumb2.jpg",
      type: "image/jpeg",
      size: 46353,
      delete_url: "http://url.to/delete /file/",
      delete_type: "DELETE"
    }
    ]
  }
  """
  case class FileUploadReturn(error: String, name: String, size: Long);
  implicit val peoplesInAgeGroupSchoolingFormat = Json.format[FileUploadReturn]
  implicit val yearCityCodesReads = Json.reads[FileUploadReturn]

  def doUpload = SecureRequest(parse.multipartFormData) { request =>
    request.body.file("document").map { document =>
      import java.io.File
      val filename = document.filename
      val file = new File(s"/tmp/document/$filename")
      managerActor ! ManagerActor.DataImportOrder(document.ref.file.getPath, request.userEmail)
      Ok(Json.obj("files" -> FileUploadReturn("", name = filename, size = file.length())))
    }.getOrElse {
      Ok(Json.obj("files" -> FileUploadReturn("", name = "No file", size = 0)))
    }
  }

}
