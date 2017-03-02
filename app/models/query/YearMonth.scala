package models.query


trait YearMonth

case class YearAndMonth(year: String, month: String) extends YearMonth
case class YearOrMonth(year: String) extends YearMonth


object YearMonth {
  def split(yearMonth: String) =
    if (yearMonth.length > 4)
      YearAndMonth(yearMonth.substring(0, 4), yearMonth.substring(4))
    else
      YearAndMonth(yearMonth.substring(0, 4), "")

  def join(year: String, month: String, separator: String = "") = year + separator + month
}