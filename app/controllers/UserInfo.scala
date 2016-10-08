package controllers

import play.api.mvc.Session

trait UserInfo {
  implicit def email(implicit session: Session): Option[String] = {
    for (email <- session.get("logged-user")) yield email
  }
}
