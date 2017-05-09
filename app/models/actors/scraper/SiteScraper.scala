package models.actors.scraper

import java.net.URL

import scala.concurrent.Future

object SiteScraper {

  import net.ruippeixotog.scalascraper.browser.JsoupBrowser
  import net.ruippeixotog.scalascraper.dsl.DSL.Extract._
  import net.ruippeixotog.scalascraper.dsl.DSL._

  private val URL = "http://www.tse.jus.br/hotSites/pesquisas-eleitorais/"
  private val MAIN_PAGE = "eleitorado.html"
  private val browser = JsoupBrowser()

  import play.api.libs.concurrent.Execution.Implicits._

  def scrapForYears(): Future[List[ScrapedYear]] = {
    Future { scrapForYearsLogic }
  }


  private def scrapForYearsLogic(): List[ScrapedYear] = {

    val mainPage = browser.get(URL + MAIN_PAGE)
    val items = mainPage >> element("#conteudo > div.navegacao_anos.span-72 > ul") >> elementList("li")
    val links = items.map(item => item >> attr("href")("a"))
    val texts = items.map(item => item >> text("a"))
    links.zip(texts)
          .map{ case (link, text) => ScrapedYear(text, URL + link) }

  }

  def scrapForContent(scrapedYear: ScrapedYear) = Future {scrapForContentLogic(scrapedYear) }

  private def scrapForContentLogic(scrapedYear: ScrapedYear): YearForDownload = {
    val pageOfYear = browser.get(scrapedYear.url)
    val downloadUrl = pageOfYear >>
                      element("#conteudo > div:nth-child(4) > p:nth-child(1)") >>
                      attr("href")("a")

    YearForDownload(scrapedYear.yearInSite, new URL(downloadUrl))

  }

}
