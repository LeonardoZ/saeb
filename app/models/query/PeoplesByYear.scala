package models.query

case class PeoplesByYear(yearMonth: String, peoples: Int)
case class PeoplesByYearGrouped(peoplesByYear: Seq[PeoplesByYear])
