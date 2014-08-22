package services

import scala.slick.driver.JdbcProfile
import scala.slick.driver.SQLiteDriver

class ArchiveService(val driver: JdbcProfile) {


  val db = Database.forUrl()

  val createResult = db.withSession { implicit session =>
    updates.ddl.create
    println("MADE DB")
  }

  def putTest(email: String) = db.withSession { implicit session =>
    updates += (email, email)
    println("Put into db")
    println(updates.iterator.mkString(","))
  }
}

object ArchiveService extends ArchiveService(SQLiteDriver)
