package models.entity

trait Status
case object Complete
case object Failed
case object Going

case class Task(id: Option[Int],
                description: String,
                userId: Int,
                completed: Boolean = false,
                failure: Boolean = false,
                message: String)
