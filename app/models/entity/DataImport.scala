package models.entity

import java.sql.Date


case class DataImport(id: Option[Int] = None,
                      importDateTime: Date,
                      fileName: String,
                      fileYear: String,
                      fileMonth: String = "")
