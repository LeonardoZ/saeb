package models.query

case class YearCityCode(year: String, code: String) {
  def hasSchoolingData() = {
    Integer.parseInt(this.year) > 1998
  }
}
