package models.entity

import java.text.Collator

case class City(id: Option[Int] = None, names: List[String], code: String, state: String, country: String) {
  def name(): String =
    names.sortWith((c1, c2) => Cities.collator.equals(c1, c2)).head

  def contentEqual(that: City): Boolean = (this.names, this.code, this.state, this.country) ==
    (that.names, that.code, that.state, that.country)

}

case class SimpleCity(id: String,  name: String, state: String, country: String) {

}

object Cities {
  val collator = Collator.getInstance
  collator.setStrength(Collator.NO_DECOMPOSITION)
  def citiesToSimpleCity(cs: Seq[City]): Iterable[SimpleCity] = {


    cs map { city =>
      if (city.names.length > 0) {
        val sortedNames = city.names.sortWith((c1, c2) => collator.equals(c1, c2))
        SimpleCity(id = city.code, state = city.state, country = city.country, name = sortedNames(0))
      } else
        SimpleCity(id = city.code, state = city.state, country = city.country, name = city.names(0))
    }
  }
}