package models.entity

import java.text.Collator

case class City(id: Option[Int] = None, name: String, code: String, state: String, country: String) {

  def contentEqual (that:City): Boolean = (this.name, this.code, this.state, this.country) ==
                                (that.name, that.code, that.state, that.country)

}

case class SimpleCity(id: String, name: String, otherNames: Seq[String], state: String)

object Cities {
  def citiesToSimpleCity(cs: Seq[City]): Iterable[SimpleCity] = {
    val collator = Collator.getInstance
    collator.setStrength(Collator.NO_DECOMPOSITION)
    cs.groupBy(_.code).map { groupedCities =>
      val sorted = groupedCities._2.sortWith((c1, c2) => collator.equals(c2.name, c1.name))
      val head = sorted.head
      val names = sorted.map(_.name)

      SimpleCity(id = head.code, name = head.name, otherNames = names, state = head.state)
    }
  }
}