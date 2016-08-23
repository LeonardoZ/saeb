package controllers

import javax.inject.Inject

import models.db.UserRepository
import models.entity.User
import models.form.SignupForm
import org.mindrot.jbcrypt.BCrypt
import play.api.data.Form
import play.api.data.Forms._
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc._

import scala.concurrent.{ExecutionContext, Future}

class UserController @Inject()(val userRepo: UserRepository, val messagesApi: MessagesApi)(implicit ec: ExecutionContext)
  extends Controller with I18nSupport {

  val signupForm: Form[SignupForm] = Form {
    mapping(
      "email" -> nonEmptyText(minLength = 6, maxLength = 255),
      "password" -> nonEmptyText(minLength = 6),
      "repeatPassword" -> nonEmptyText(minLength = 6)
    )(SignupForm.apply)(SignupForm.unapply)
  }


  def newUser() = Action.async { implicit request =>
    Future {
      Ok(views.html.new_user(signupForm))
    }
  }

  def users() = Action.async { implicit request =>
    userRepo.getAll.flatMap { users =>
      Future {
        Ok(views.html.users(users))
      }
    }
  }


  def register() = Action.async { implicit request =>

    signupForm.bindFromRequest.fold(
      errorForm => Future {
        Redirect(routes.Application.signup())
      },
      form => {
        val salt = BCrypt.gensalt()
        val pass = BCrypt.hashpw(form.password, salt)
        val user = User(None, form.email, pass, false)

        userRepo.create(user) map { f =>
          Redirect(routes.Application.signin())
        }
      }
    )
  }

}
