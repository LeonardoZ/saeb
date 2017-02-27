package models.db

import com.github.tminglei.slickpg._

trait MyPostgresDriver extends ExPostgresDriver with PgArraySupport {
  override val api: API = new API {}

  trait API extends super.API
    with ArrayImplicits
    with SimpleArrayImplicits
    with SimpleArrayPlainImplicits {
      implicit val varcharToList = new SimpleArrayJdbcType[String]("varchar").to(_.toList)
      implicit val simpleVarcharToList = new SimpleArrayJdbcType[String]("text")
    }

}

object MyPostgresDriver extends MyPostgresDriver  {
}
