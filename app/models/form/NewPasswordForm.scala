package models.form

case class NewPasswordForm(password: String, newPassword: String, repeatedNewPassword: String)

case class NewSimplesPasswordForm(jwt: String, email: String, newPassword: String, repeatedNewPassword: String)
