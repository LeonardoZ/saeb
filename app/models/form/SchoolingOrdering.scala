package models.form

case class SchoolingOrder(id: Int, index: Int)
case class Orders(orders: Seq[SchoolingOrder])
