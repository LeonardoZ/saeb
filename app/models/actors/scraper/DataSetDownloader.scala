package models.actors.scraper

import java.io.{File, FileInputStream, FileOutputStream, IOException}
import java.util.zip.{ZipEntry, ZipInputStream}
import java.util.{Date, UUID}

import scala.concurrent.Future

object DataSetDownloader {

  import sys.process._

  def download(yearForDownload: YearForDownload): Future[DownloadedYear] = {

    import play.api.libs.concurrent.Execution.Implicits._

    Future {
      val tempFile = createTempFile(yearForDownload.year)
      val url = yearForDownload.url
      val conn = url.openConnection
      conn.setRequestProperty("Accept", "text/json")
      conn.setIfModifiedSince(new Date().getTime - 1000 * 60 * 30)
      url #> tempFile !!

      val tempFolder = createTempFolder(tempFile.getName)
      unzip(tempFile, tempFolder)
      DownloadedYear(yearForDownload.year, getFileInFolder(tempFolder).getAbsolutePath, true)
    }
  }

  private def getFileInFolder(folder: File): File = {
    folder.listFiles().sortBy(_.length()).reverse.head
  }

  /**
    * Thnks to anquegi on http://stackoverflow.com/questions/30640627/how-to-unzip-a-zip-file-using-scala
    */
  private def unzip(zipFile: File, outputFolder: File): Unit = {

    val buffer = new Array[Byte](1024)

    try {

      //zip file content
      val zis: ZipInputStream = new ZipInputStream(new FileInputStream(zipFile));
      //get the zipped file list entry
      var ze: ZipEntry = zis.getNextEntry();

      while (ze != null) {

        val fileName = ze.getName();
        val newFile = new File(outputFolder + File.separator + fileName);

        println("file unzip : " + newFile.getAbsoluteFile());

        //create folders
        new File(newFile.getParent()).mkdirs();

        val fos = new FileOutputStream(newFile);

        var len: Int = zis.read(buffer);

        while (len > 0) {

          fos.write(buffer, 0, len)
          len = zis.read(buffer)
        }

        fos.close()
        ze = zis.getNextEntry()
      }

      zis.closeEntry()
      zis.close()

    } catch {
      case e: IOException => println("exception caught: " + e.getMessage)
    }

  }

  private def createTempFolder(fileName: String): File = {
    val uuid = UUID.randomUUID().toString
    val tmp = System.getProperty("java.io.tmpdir")
    val f = new File(s"${tmp}/${fileName}_f")
    f.mkdir()
    f
  }

  private def createTempFile(prepend: String): File = {
    val uuid = UUID.randomUUID().toString
    val tmp = System.getProperty("java.io.tmpdir")
    val f = new File(s"${tmp}/saeb_${prepend}_${uuid}")
    f.createNewFile()
    f
  }

}
