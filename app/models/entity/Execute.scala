package models.entity

import scala.collection.mutable.HashSet
object Execute {



  case class Profile(yearOrMonth: String,
                     city: City,
                     electoralDistrict: String,
                     sex: Sex,
                     ageGroup: AgeGroup,
                     schooling: Schooling,
                     quantityOfPeoples: Int)

  val exteriorCityPattern = ("\\w+-\\w{2,5}").r
  val yearColumn = 0
  val stateColumn = yearColumn + 1
  val cityColumn = stateColumn + 1
  val cityCodeColumn = cityColumn + 1
  val electoralDistrictColumn = cityCodeColumn + 1
  val sexColumn = electoralDistrictColumn + 1
  val ageGroupColumn = sexColumn + 1
  val schoolingGroupColumn = ageGroupColumn + 1
  val quantityColumn = schoolingGroupColumn + 1
  val cityCountryPattern = "\\w+-\\w{2,5}".r
  val notInformed = "NÃO INFORMADO"

  def cityColumnParser(line: Array[String]): City = line(stateColumn) match {
    case "ZZ" => {
      val city = line(cityColumn)
      val containCity = cityCountryPattern.findFirstIn(city).isDefined
      if (containCity) {
        val cityCountry = city.split(";")
        City(code = line(cityCodeColumn), name = cityCountry(0), state = line(stateColumn), country = cityCountry(1))
      } else {
        City(code = line(cityCodeColumn), name = notInformed, state = line(stateColumn), country = city)
      }
    }
    case _ => City(code = line(cityCodeColumn), name = line(cityColumn), state = line(stateColumn), country = "Brazil")
  }

  def persistValueColumns(arr: Array[String]) = {
    val city = cityColumnParser(arr)
    val schooling = schoolingColumnParser(arr)
    val age = ageGroupColumnParser(arr)

  }



  def yearColumnParser(value: Array[String]): String = value(yearColumn)

  def districtColumnParser(value: Array[String]): String = value(electoralDistrictColumn)

  def ageGroupColumnParser(value: Array[String]): AgeGroup = AgeGroup(group = value(ageGroupColumn))

  def schoolingColumnParser(value: Array[String]): Schooling = Schooling(level = value(schoolingGroupColumn))

  def peopleCountColumnParser(value: Array[String]): Int = Integer.valueOf(value(quantityColumn))

  def sexColumnParser(value: Array[String]): Sex = value(sexColumn) match {
    case "FEMININO" => Female
    case "MASCULINO" => Male
    case _ => NotDefined
  }

  def fullLineParser(arr: Array[String]) =
    Profile(
      yearOrMonth = yearColumnParser(arr),
      city = cityColumnParser(arr),
      electoralDistrict = districtColumnParser(arr),
      ageGroup = ageGroupColumnParser(arr),
      schooling = schoolingColumnParser(arr),
      quantityOfPeoples = peopleCountColumnParser(arr),
      sex = sexColumnParser(arr)
    )


  def main(args: Array[String]) = {
    val address = "C:\\Users\\Leonardo\\Desktop\\Perfil_eleitorado\\perfil_eleitorado_2006.txt"
    val ageGroupSet = HashSet[AgeGroup]()
    val schoolingSet = HashSet[Schooling]()
    val citySet = HashSet[City]()
    val values = scala.io.Source.fromFile(address, "latin1")
      .getLines
      .map { line =>
        val cleanLine = line.replace("\"", "").split(";")
      }

    val profiles: Iterator[Profile] = scala.io.Source.fromFile(address, "latin1")
      .getLines
      .map { line =>
        val cleanLine = line.replace("\"", "").split(";")
        val profile = fullLineParser(cleanLine)
        ageGroupSet + profile.ageGroup
        schoolingSet + profile.schooling
        citySet + profile.city
        profile
      }
    profiles.foreach(println(_))
  }


}
