package models.query

case class PeoplesByYearAndSex(yearMonth: String, sex: String, peoples: Int)

case class PeoplesByYearAndSexGrouped(yearMonth: String, peoplesBySex: Seq[PeoplesBySex])

case class PeoplesBySex(sex: String, peoples: Int)
