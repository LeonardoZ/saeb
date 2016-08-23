package controllers

import javax.inject.Inject

import controllers.security.SecureRequest
import models.db.UserRepository
import models.entity.User
import models.form.{LoginForm, SignupForm}
import org.mindrot.jbcrypt.BCrypt
import play.api.data.Form
import play.api.data.Forms._
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc._

import scala.concurrent.{ExecutionContext, Future}

class Application @Inject()(val userRepo: UserRepository, val messagesApi: MessagesApi)(implicit ec: ExecutionContext)
  extends Controller with I18nSupport {


  val loginForm: Form[LoginForm] = Form {
    mapping(
      "login" -> email,
      "password" -> nonEmptyText(minLength = 6),
      "remember" -> boolean
    )(LoginForm.apply)(LoginForm.unapply)
  }

  val signupForm: Form[SignupForm] = Form {
    mapping(
      "email" -> nonEmptyText(minLength = 6, maxLength = 255),
      "password" -> nonEmptyText(minLength = 6),
      "repeatPassword" -> nonEmptyText(minLength = 6)
    )(SignupForm.apply)(SignupForm.unapply)
  }


  def signin(emailValue: String = "") = Action.async { implicit request =>
    loginForm.fill(LoginForm(login = emailValue, password = "", remember = false))
    userRepo.countUsers flatMap { users =>
      Future {
        Ok(views.html.login(loginForm, signupForm, users < 1))
      }
    }
  }

  val invalidCredentials =
    Future {
      Redirect(routes.Application.signin()).flashing(("login", "Credenciais inválidas."))
    }

  def doLogin() = Action.async { implicit request =>
    loginForm.bindFromRequest.fold(
      errorForm => invalidCredentials,
      form => {
        val aFutureUser = userRepo.getUser(form.login)
        aFutureUser.flatMap {
          case Some(u) =>
            if (BCrypt.checkpw(form.password, u.password))
              authenticatedUser(u, request)
            else invalidCredentials
          case None => invalidCredentials
        }
      }
    )
  }

  def doLogout() = SecureRequest.async { implicit request =>
    Future {
      Redirect(routes.Application.signin()).withNewSession
    }
  }

  def authenticatedUser(user: User, request: Request[AnyContent]): Future[Result] = {
    Future {
      Redirect(routes.MainController.index())
        .withNewSession.withSession(request.session + ("logged-user", user.email))
    }
  }

  def signup() = Action.async { implicit request =>

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
