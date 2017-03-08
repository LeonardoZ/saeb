name := """saeb"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.7"

routesGenerator := InjectedRoutesGenerator

resolvers += "scalaz-bintray" at "https://dl.bintray.com/scalaz/releases"

resolvers += "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots/"

libraryDependencies ++= Seq(
  "com.adrianhurt" %% "play-bootstrap" % "1.1-P25-B3",
  "org.webjars" % "font-awesome" % "4.5.0",
  "org.webjars" % "bootstrap-datepicker" % "1.4.0",
  "com.typesafe.play" %% "play-slick" % "2.0.2",
  "com.typesafe.play" %% "play-slick-evolutions" % "2.0.2",
  "net.ruippeixotog" %% "scala-scraper" % "1.0.0",
  "org.postgresql" % "postgresql" % "9.4.1212",
  "com.github.tminglei" %% "slick-pg" % "0.14.6",
  "com.github.t3hnar" %% "scala-bcrypt" % "2.6",
  "org.scalatestplus.play" %% "scalatestplus-play" % "1.5.0" % "test",
  "com.typesafe.play" %% "play-mailer" % "5.0.0",
  "com.jason-goodwin" %% "authentikat-jwt" % "0.4.1",
  cache,
  specs2 % Test
)

fork in run := true

maintainer in Linux := "Leonardo Zapparoli <leo.zapparoli@gmail.com>"

packageSummary in Linux := "SAEB for Debian-like Servers =)"

packageDescription := "Software de análise do eleitorado brasileiro"