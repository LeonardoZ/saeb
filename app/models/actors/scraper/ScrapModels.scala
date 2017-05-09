package models.actors.scraper

import java.net.URL

case class ScrapedYear(yearInSite: String, url: String)

case class YearForDownload(year: String, url: URL)

case class DownloadedYear(yearInSite: String, pathToFileInTemp: String, success: Boolean)
