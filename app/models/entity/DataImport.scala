package models.entity

import java.sql.Timestamp


case class DataImport(id: Option[Int] = None,
                      importDateTime: Timestamp,
                      fileName: String,
                      fileYear: String,
                      fileMonth: String = "",
                      userId: Int) {
  def yearMonth = fileYear + fileMonth
}
