package accounts.model

import play.api.db.slick.HasDatabaseConfigProvider
import slick.driver.JdbcProfile

trait AccountTable { self: HasDatabaseConfigProvider[JdbcProfile] =>

  import driver.api._

  class AccountTable(tag: Tag) extends Table[Account](tag, "account") {
    val name: Rep[String] = column[String]("name", O.PrimaryKey, O.SqlType("VARCHAR(200)"))
    var password: Rep[String] = column[String]("password", O.SqlType("VARCHAR(200)"))
    def * = (name, password) <> (Account.tupled, Account.unapply)
  }

  lazy protected val accounts = TableQuery[AccountTable]

  lazy protected val accountsInc = accounts returning accounts.map(_.name)

}
