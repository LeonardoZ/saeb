package controllers

import javax.inject.Inject

import models.db.UserRepository
import models.entity.{User, UserPasswordManager}
import models.form._
import models.mail.{ForgotPasswordEmail, MailComponent}
import org.mindrot.jbcrypt.BCrypt
import play.api.data.Form
import play.api.data.Forms._
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc._

import scala.concurrent.{ExecutionContext, Future}

class Application @Inject()(val userRepo: UserRepository,
                            val messagesApi: MessagesApi,
                            val configuration: play.api.Configuration,
                            val emailComponent: MailComponent)(implicit ec: ExecutionContext)
  extends Controller with I18nSupport {


  val loginForm: Form[LoginForm] = Form {
    mapping(
      "login" -> email,
      "password" -> nonEmptyText(minLength = 6)
    )(LoginForm.apply)(LoginForm.unapply)
  }

  val forgotPasswordForm: Form[ForgotPasswordForm] = Form {
    mapping(
      "email" -> email
    )(ForgotPasswordForm.apply)(ForgotPasswordForm.unapply)
  }

  val newPasswordForm: Form[NewSimplePasswordForm] = Form {
    mapping(
      "jwt" -> nonEmptyText,
      "email" -> email,
      "newPassword" -> nonEmptyText(minLength = 6),
      "repeatNewPassword" -> nonEmptyText(minLength = 6)
    )(NewSimplePasswordForm.apply)(NewSimplePasswordForm.unapply)
  }

  val reactivateForm: Form[ReactivateForm] = Form {
    mapping(
      "jwt" -> nonEmptyText,
      "email" -> email
    )(ReactivateForm.apply)(ReactivateForm.unapply)
  }

  val signupForm: Form[SignupForm] = Form {
    mapping(
      "email" -> email,
      "password" -> nonEmptyText(minLength = 6),
      "repeatPassword" -> nonEmptyText(minLength = 6)
    )(SignupForm.apply)(SignupForm.unapply)
  }


  def signin(emailValue: String = "") = Action.async { implicit request =>
      println(System.getenv("SAEB_DB_USER"))
      println(System.getenv("SAEB_DB_NAME"))
    loginForm.fill(LoginForm(login = emailValue, password = ""))
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
          case Some(u) if BCrypt.checkpw(form.password, u.password) => authenticatedUser(u, request)
          case _ => invalidCredentials
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

  def forgotPassword = Action.async { implicit request =>
    forgotPasswordForm.bindFromRequest.fold(
      errorForm => Future {
        Redirect(routes.Application.signin())
          .flashing(("forgotPasswordEmailError" -> "Erro ao processar requisição."))
      },
      form => {
        val secret = configuration.getString("play.crypto.secret").getOrElse("XXX")

        userRepo.getUser(form.email) flatMap {
          case Some(user) => for {
            url <- Future {
              UserPasswordManager.generateEmailAndCreatedJwt(user.email, secret)
            }
            email <- Future { ForgotPasswordEmail(host = request.host, url = s"admin/forgot/$url", to = user.email) }
            sendEmail <- emailComponent.sendEmail(email)
            redirect <- Future {
              Redirect(routes.Application.signin()).flashing(("forgotPasswordEmailOk" -> "E-mail enviado ao destinatário."))
            }
          } yield (redirect)
          case None => Future {
            Redirect(routes.Application.signin())
              .flashing(("forgotPasswordEmailError" -> "E-mail não cadastrado."))
          }
        }
      }
    )
  }

  def changePasswordPage(jwt: String) = Action.async { implicit request =>
    val secret = configuration.getString("play.crypto.secret").getOrElse("XXX")

    UserPasswordManager.emailFromJwt(jwt, secret) match {
      case Some(email) => Future {
        Ok {
          val newForm = newPasswordForm.fill(NewSimplePasswordForm(jwt = jwt, email = email, "", ""))
          views.html.new_password_remember(newForm)
        }
      }
      case None => Future {
        Redirect(routes.Application.signin()).flashing(("forgotPasswordEmailError" ->
          "Falha ao tentar reativar sua conta. E-mail não encontrado"))
      }
    }
  }

  def reactivatePage(jwt: String) = Action.async { implicit request =>
    val secret = configuration.getString("play.crypto.secret").getOrElse("XXX")

    UserPasswordManager.emailFromJwt(jwt, secret) match {
      case Some(email) => Future {
        val newForm = reactivateForm.fill(ReactivateForm(jwt = jwt, email = email))
        Ok {
          views.html.reactivate_account(newForm)
        }
      }
      case None => Future {
        Redirect(routes.Application.signin()).flashing(("reactivateError" ->
          "Falha ao tentar reativar sua conta. E-mail não encontrado"))
      }
    }
  }

  def saveNewPassword = Action.async { implicit request =>
    newPasswordForm.bindFromRequest.fold(
      errorForm => Future {
        Redirect(routes.Application.signin()).flashing(("forgotPasswordEmailError" -> "Erro ao processar requisição."))
      },
      form => {
        val secret = configuration.getString("play.crypto.secret").getOrElse("XXX")
        UserPasswordManager.emailFromJwt(form.jwt, secret) match {
          case Some(email) if email == form.email => processUpdate(form)
          case _ => Future {
            Redirect(routes.Application.signin())
              .flashing(("forgotPasswordEmailError" -> "E-mail não cadastrado nessa operação."))
          }
        }
      }
    )
  }


  def reactivate = Action.async { implicit request =>
    reactivateForm.bindFromRequest.fold(
      errorForm => Future {
        Redirect(routes.Application.signin()).flashing(("reactivateError" -> "Erro ao processar requisição."))
      },
      form => {
        val secret = configuration.getString("play.crypto.secret").getOrElse("XXX")
        UserPasswordManager.emailFromJwt(form.jwt, secret) match {
          case Some(email) if email == form.email => processReactivation(form)
          case _ => Future {
            Redirect(routes.Application.signin())
              .flashing(("reactivateError" -> "E-mail não cadastrado nessa operação."))
          }
        }
      }
    )
  }

  def processUpdate(form: NewSimplePasswordForm): Future[Result] = {
    userRepo.updatePassword(form.email, UserPasswordManager.encryptPassword(form.newPassword)).map { x =>
      Redirect(routes.Application.signin())
        .flashing(("forgotPasswordEmailOk" -> "Senha alterada com sucesso."))
    }
  }

  def processReactivation(form: ReactivateForm): Future[Result] = {
    userRepo.setUserActive(form.email).map { x =>
      Redirect(routes.Application.signin())
        .flashing(("reactivate" -> "Conta reativada com sucesso."))
    }
  }


  def signup() = Action.async { implicit request =>

    signupForm.bindFromRequest.fold(
      errorForm => Future {
        Redirect(routes.Application.signin())
      },
      form => {
        val pass = UserPasswordManager.encryptPassword(form.password)
        val user = User(None, form.email, pass, true)

        userRepo.create(user) map { f =>
          Redirect(routes.Application.signin())
        }
      }
    )
  }

  def loggedSignup() = Action.async { implicit request =>

    signupForm.bindFromRequest.fold(
      errorForm => Future {
        Redirect(routes.Application.signup())
      },
      form => {
        val salt = BCrypt.gensalt()
        val pass = BCrypt.hashpw(form.password, salt)
        val user = User(None, form.email, pass, true)

        userRepo.create(user) map { f =>
          Redirect(routes.UserController.register())
        }
      }
    )
  }

}
