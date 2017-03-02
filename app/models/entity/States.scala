package models.entity


class State(val name: String, val federativeUnit: String, val region: String) {
  def upperCaseFederativeUnit() = federativeUnit.toUpperCase
  def lowerCaseFederativeUnit() = federativeUnit.toLowerCase
}

case object Acre extends State("Acre", "AC", "NORTE")
case object Alagoas extends State("Alagoas", "AL", "NORDESTE")
case object Amapa extends State("Amapá", "AP", "NORTE")
case object Amazonas extends State("Amazonas", "AM", "NORTE")
case object Bahia extends State("Bahia", "BA", "NORDESTE")
case object Ceara extends State("Ceará", "CE", "NORDESTE")
case object DistritoFederal extends State("Distrito Federal", "DF", "CENTROOESTE")
case object EspiritoSanto extends State("Espírito Santo", "ES", "SUDESTE")
case object Goias extends State("Goiás", "GO", "CENTROOESTE")
case object Maranhao extends State("Maranhão", "MA", "NORDESTE")
case object MatoGrosso extends State("Mato Grosso", "MT", "CENTROOESTE")
case object MatoGrossoDoSul extends State("Mato Grosso do Sul", "MS", "CENTROOESTE")
case object MinasGerais extends State("Minas Gerais", "MG", "SUDESTE")
case object Para extends State("Pará", "PA", "NORTE")
case object Paraiba extends State("Paraíba", "PB", "NORDESTE")
case object Parana extends State("Paraná", "PR", "SUL")
case object Pernambuco extends State("Pernambuco", "PE", "NORDESTE")
case object Piaui extends State("Piauí", "PI", "NORDESTE")
case object RioDeJaneiro extends State("Rio de Janeiro", "RJ", "SUDESTE")
case object RioGrandeDoNorte extends State("Rio Grande do Norte", "RN", "NORDESTE")
case object RioGrandeDoSul extends State("Rio Grande do Sul", "RS", "SUL")
case object Rondonia extends State("Rondônia", "RO", "NORTE")
case object Roraima extends State("Roraima", "RR", "NORTE")
case object SantCatarina extends State("Santa Catarina", "SC", "SUL")
case object SaoPaulo extends State("São Paulo", "SP", "SUDESTE")
case object Sergipe extends State("Sergipe", "SE", "NORDESTE")
case object Tocantins extends State("Tocantins", "TO", "NORTE")

object States {

  def getByFederativeUnit(federativeUnit: String): Option[State] = {
    all.find(_.federativeUnit.equalsIgnoreCase(federativeUnit))
  }

  val all: Seq[State] = Seq(
    Acre,
    Alagoas,
    Amapa,
    Amazonas,
    Bahia,
    Ceara,
    DistritoFederal,
    EspiritoSanto,
    Goias,
    Maranhao,
    MatoGrosso,
    MatoGrossoDoSul,
    MinasGerais,
    Para,
    Paraiba,
    Parana,
    Pernambuco,
    Piaui,
    RioDeJaneiro,
    RioGrandeDoNorte,
    RioGrandeDoSul,
    Rondonia,
    Roraima,
    SantCatarina,
    SaoPaulo,
    Sergipe,
    Tocantins)
}
