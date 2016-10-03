package controllers

import javax.inject.Inject

import controllers.security.SecureRequest
import models.db._
import models.form.SchoolingOrder
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.libs.json.{JsError, Json, Reads}
import play.api.mvc.{BodyParsers, Controller}

import scala.concurrent.{ExecutionContext, Future}

class SchoolingController @Inject()(val schoolingRepository: SchoolingRepository,
                                    val messagesApi: MessagesApi)(implicit ec: ExecutionContext)
  extends Controller with I18nSupport {

  case class Orders(orders: Seq[SchoolingOrder])

  implicit val orderFormat = Json.format[SchoolingOrder]
  implicit val ordersFormat = Reads.seq(orderFormat)


  def classification = SecureRequest.async {
    schoolingRepository.getAll().flatMap { schoolings =>
      Future {

        Ok(views.html.schooling_classification(schoolings))
      }
    }
  }


  def saveClassification = SecureRequest.async(BodyParsers.parse.json) { implicit request =>
    val jsonResult = request.body.validate(ordersFormat)
    jsonResult.fold(
      error => {
        Future {
          BadRequest(Json.obj("status" -> 400, "messages" -> JsError.toJson(error)))
        }
      },
      classifications => {
        val toPersist = Future.sequence(classifications.map(item => schoolingRepository.getById(item.id).map {
          case Some(sch) => Option {
            schoolingRepository.update(sch.copy(position = item.index))
          }
          case None => None
        }))

        toPersist.flatMap { result =>
          Future {
            Ok(Json.obj("inserted" -> "Ok"))
          }
        }
      })
  }

}
