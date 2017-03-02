package models.form

case class NewPasswordForm(password: String, newPassword: String, repeatedNewPassword: String)

case class NewSimplePasswordForm(jwt: String, email: String, newPassword: String, repeatedNewPassword: String)
