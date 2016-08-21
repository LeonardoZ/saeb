package models.entity


object SexParser {
  def parse(value: String) = value match {
    case "FEMININO" => Female
    case "MASCULINO" => Male
    case _ => NotDefined
  }

  def convert(sex: Sex) = sex match {
    case Female => "F"
    case Male => "M"
    case NotDefined => "N"
  }
}

trait Sex

case object Female extends Sex;

case object Male extends Sex;

case object NotDefined extends Sex;


