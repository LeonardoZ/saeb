package models.db

import com.github.tminglei.slickpg._
import slick.driver.JdbcProfile
import slick.profile.Capability

trait MyPostgresDriver extends ExPostgresDriver
  with PgArraySupport
  with PgRangeSupport
  with PgHStoreSupport
  with PgSearchSupport
  with PgNetSupport
  with PgLTreeSupport {
  def pgjson = "jsonb" // jsonb support is in postgres 9.4.0 onward; for 9.3.x use "json"

  // Add back `capabilities.insertOrUpdate` to enable native `upsert` support; for postgres 9.5+
  override protected def computeCapabilities: Set[Capability] =
  super.computeCapabilities + JdbcProfile.capabilities.insertOrUpdate

  override val api = MyAPI

  object MyAPI extends API with ArrayImplicits
    with NetImplicits
    with LTreeImplicits
    with RangeImplicits
    with HStoreImplicits
    with SearchImplicits
    with SearchAssistants {
    implicit val strListTypeMapper = new SimpleArrayJdbcType[String]("text").to(_.toList)
  }
}

object MyPostgresDriver extends MyPostgresDriver