package controllers

import javax.inject.Inject

import controllers.security.SecureRequest
import models.db.UserRepository
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.Controller

import scala.concurrent.ExecutionContext

class MainController@Inject()(val userRepo: UserRepository, val messagesApi: MessagesApi)(implicit ec: ExecutionContext)
  extends Controller with I18nSupport {

  def index = SecureRequest {
    Ok("Wololo")
  }

}
