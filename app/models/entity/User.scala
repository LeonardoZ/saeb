package models.entity

/**
  * Created by Leonardo on 11/07/2016.
  */
case class User(id: Option[Int], email: String, password: String, remember: Boolean)
