package accounts.model

import com.google.inject.{Inject, Singleton}

import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.driver.JdbcProfile

import scala.concurrent.Future


@Singleton()
class AccountRepository @Inject()(protected val dbConfigProvider: DatabaseConfigProvider)
  extends AccountTable with HasDatabaseConfigProvider[JdbcProfile] {

  import driver.api._

  def signUp(account: Account): Future[Int] =
    db.run { accounts += account }

  def getByName(name: String): Future[Option[Account]] =
    db.run { accounts.filter(_.name === name).result.headOption }

  def getAll: Future[List[Account]] =
    db.run { accounts.to[List].result }

  def ddl = accounts.schema

}

