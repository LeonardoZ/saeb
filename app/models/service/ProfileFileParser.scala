package models.service

import java.nio.file.Paths
import java.sql.Timestamp
import java.time.LocalDateTime
import javax.inject.Inject

import models.entity._

import scala.io.Source


object ProfileFileParser {
  val exteriorCityPattern = ("\\w+-\\w{2,5}").r
  val yearColumn = 0
  val stateColumn = 1
  val cityColumn = 2
  val cityCodeColumn = 3
  val electoralDistrictColumn = 4
  val sexColumn = 5
  val ageGroupColumn = 6
  val schoolingGroupColumn = 7
  val quantityColumn = 8
  val cityCountryPattern = "\\w+-\\w{2,5}".r
  val cleanPattern = "\"".intern
  val notInformed = "NÃO INFORMADO"
  val defaultCountry = "Brazil"
}

class ProfileFileParser @Inject()() {

  import ProfileFileParser._

  def parseProfile(path: String): Stream[FullProfile] = {
    Source.fromFile(path, "latin1")
      .getLines
      .map { line =>
        val cleanLine = line.replaceAll(cleanPattern, "").split(";")
        fullLineParser(cleanLine)
      }.toStream
  }

  def parseValues(path: String): (Set[SimpleCity], Set[AgeGroup], Set[Schooling]) = {
    val mappedValues = Source.fromFile(path, "latin1")
      .getLines
      .map { line =>
        val cleanLine = line.replaceAll(cleanPattern, "").split(";")
        val city = cityColumnParser(cleanLine)
        val age = ageGroupColumnParser(cleanLine)
        val schooling = schoolingColumnParser(cleanLine)
        (city, age, schooling)
      }.toList

    val cities = mappedValues.map(_._1).toSet
    val ages = mappedValues.map(_._2).toSet
    val schoolings = mappedValues.map(_._3).toSet

    (cities, ages, schoolings)
  }

  def parseFileData(path: String, userId: Int): DataImport = {
    val yearCol = Source.fromFile(path, "latin1")
      .getLines
      .map { line =>
        val cleanLine = line.replaceAll(cleanPattern, "").split(";")
        cleanLine(yearColumn)
      }.toSet.head

    val yearMonth = if (yearCol.length == 6)
      (yearCol.substring(0, 4), yearCol.substring(5))
    else
      (yearCol.substring(0, 4), "")

    val fileNameExtracted = Paths.get(path).getFileName.toString
    DataImport(fileName = fileNameExtracted,
      importDateTime = Timestamp.valueOf(LocalDateTime.now()),
      fileYear = yearMonth._1,
      fileMonth = yearMonth._2,
      userId = userId)
  }

  private def fullLineParser(arr: Array[String]) =
    FullProfile(
      yearOrMonth = yearColumnParser(arr),
      city = cityColumnParser(arr),
      electoralDistrict = districtColumnParser(arr),
      ageGroup = ageGroupColumnParser(arr),
      schooling = schoolingColumnParser(arr),
      quantityOfPeoples = peopleCountColumnParser(arr),
      sex = sexColumnParser(arr)
    )

  private def cityColumnParser(line: Array[String]): SimpleCity = line(stateColumn) match {
    case "ZZ" => {
      val city = line(cityColumn).intern
      val containCity = cityCountryPattern.findFirstIn(city).isDefined
      if (containCity) {
        val cityCountry = city.split("-")

        SimpleCity(id = line(cityCodeColumn), name = cityCountry(0).intern(), state = line(stateColumn),
          country = cityCountry(1).intern)
      } else {
        SimpleCity(id = line(cityCodeColumn), name = notInformed, state = line(stateColumn).intern, country = city)
      }
    }
    case _ => SimpleCity(id = line(cityCodeColumn),
      name = line(cityColumn).intern, state = line(stateColumn).intern, country = defaultCountry)
  }

  private def yearColumnParser(line: Array[String]): String = line(yearColumn).intern

  private def districtColumnParser(line: Array[String]): String = line(electoralDistrictColumn).intern

  private def ageGroupColumnParser(line: Array[String]): AgeGroup =
    AgeGroup(group = line(ageGroupColumn).intern)

  private def schoolingColumnParser(line: Array[String]): Schooling = Schooling(level = line(schoolingGroupColumn).intern)

  private def peopleCountColumnParser(line: Array[String]): Int = Integer.valueOf(line(quantityColumn))

  def sexColumnParser(line: Array[String]): Sex = SexParser.parse(line(sexColumn))


}
