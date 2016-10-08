package controllers

import javax.inject.Inject

import models.db.UserRepository
import models.entity.{User, UserPasswordManager}
import models.form.{NewPasswordForm, SignupForm}
import play.api.data.Form
import play.api.data.Forms._
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc._

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

class UserController @Inject()(val userRepo: UserRepository,
                               val messagesApi: MessagesApi)(implicit ec: ExecutionContext)
  extends Controller with I18nSupport with UserInfo {

  val signupForm: Form[SignupForm] = Form {
    mapping(
      "email" -> nonEmptyText(minLength = 6, maxLength = 255),
      "password" -> nonEmptyText(minLength = 6),
      "repeatPassword" -> nonEmptyText(minLength = 6)
    )(SignupForm.apply)(SignupForm.unapply)
  }

  val changePasswordForm: Form[NewPasswordForm] = Form {
    mapping(
      "password" -> nonEmptyText(minLength = 6),
      "newPassword" -> nonEmptyText(minLength = 6),
      "repeatNewPassword" -> nonEmptyText(minLength = 6)
    )(NewPasswordForm.apply)(NewPasswordForm.unapply)
  }


  def newUser() = SecureRequest.async { implicit request =>
    Future {
      Ok(views.html.new_user(signupForm))
    }
  }

  def users() = SecureRequest.async { implicit request =>
    (for {
      users <- userRepo.getAll
      inactiveUsers <- userRepo.getAllInactive
    } yield (users, inactiveUsers)) map {
      case (users, inactives) => Ok(views.html.users(users, inactives))
    }
  }

  def user() = SecureRequest.async { implicit request =>
    userRepo.getUserUnsafe(request.userEmail).flatMap { user =>
      Future {
        Ok(views.html.user_profile(user))
      }
    }
  }

  def setUserInactive() = SecureRequest.async { implicit request =>
    userRepo.setUserInactive(request.userEmail).map { result =>
      Redirect(routes.Application.signin()).withNewSession
    }
  }

  val changePasswordDefaultError = Future {
    Redirect(routes.UserController.user())
      .flashing(("changePasswordError", "Erro no processo de alteração de senha. Tente novamente"))
  }

  val changePasswordNotEqualError = Future {
    Redirect(routes.UserController.user())
      .flashing(("changePasswordError", "Senhas não confererem. Entre com sua nova senha e a repetição novamente."))
  }

  def changeUserPassword = SecureRequest.async { implicit request => {
    changePasswordForm.bindFromRequest.fold(
      error => changePasswordDefaultError,
      form => processChange(request.userEmail, form)
    )
  }
  }

  def processChange(email: String, form: NewPasswordForm): Future[Result] = {
    userRepo.getUser(email).flatMap {
      case Some(user) if UserPasswordManager.checkItAll(form, user) => updateUser(user, form.newPassword)
      case _ => {
        changePasswordDefaultError
      }
    }
  }

  def updateUser(user: User, newPassword: String): Future[Result] = {
    println("Im here")
    val f: (User) => Future[Result] = userRepo.updatePassword(_, UserPasswordManager.encryptPassword(newPassword)).flatMap {
      case Success(updated) => Future {
        Redirect(routes.Application.signin())
          .flashing(("changePasswordOk" -> "Senha alterada com sucesso. Entre com sua nova senha."))
          .withNewSession
      }
      case Failure(ex) => changePasswordDefaultError
    }
    f(user)
  }

  def sendReactivationEmailFor(email: String) = SecureRequest.async { implicit request =>
    Future {
      Redirect(routes.UserController.users()).flashing(
        ("reactivation" -> s"E-mail enviado para usuário ${email}"))
    }
  }

  def register() = Action.async { implicit request =>
    signupForm.bindFromRequest.fold(
      errorForm => Future {
        Redirect(routes.Application.signup())
      },
      form => {
        val pass = UserPasswordManager.encryptPassword(form.password)
        val user = User(None, form.email, pass, false)

        userRepo.create(user) map { f =>
          Redirect(routes.Application.signin())
        }
      }
    )
  }

}