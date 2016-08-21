package models.entity

case class City(id: Option[Int] = None, name: String, code: String, state: String, country: String) {

  def contentEqual (that:City): Boolean = (this.name, this.code, this.state, this.country) ==
                                (that.name, that.code, that.state, that.country)

}
