package repo

import accounts.model.{Account, AccountRepository}
import play.api.Application
import play.api.test.{PlaySpecification, WithApplication}

import scala.concurrent.{Await, Future}
import scala.concurrent.duration.Duration


class AccountRepositorySpec extends PlaySpecification {

  "Account repository" should {

    def accountRepo(implicit app: Application) = Application.instanceCache[AccountRepository].apply(app)


    "get all rows" in new WithApplication()  {
      val result = await(accountRepo.getAll)
      result.length === 1
      result.head.name === "orestis"
      result.head.password === "1234"
    }

    "get single rows" in new WithApplication() {
      val result = await(accountRepo.getByName("orestis"))
      result.isDefined === true
      result.get.password === "1234"
    }

    "sign up" in new WithApplication()  {
      val knolId = await(accountRepo.signUp(Account("Emilia", "24")))
      knolId === 1
    }

  }

  def await[T](v: Future[T]): T = Await.result(v, Duration.Inf)

}
