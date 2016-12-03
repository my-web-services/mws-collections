package repo


import books.Book
import books.Book
import books.Book.BookRepository
import play.api.Application
import play.api.test.{PlaySpecification, WithApplication}

import scala.concurrent.duration.Duration
import scala.concurrent.Await
import scala.concurrent.Future


class BookRepositorySpec extends PlaySpecification {

  "Book repository" should {

    def bookRepo(implicit app: Application) = Application.instanceCache[BookRepository].apply(app)


    "get all rows" in new WithApplication()  {
      val result = await(bookRepo.getAll)
      result.length === 1
      result.head.title === "The Rebel"
    }

    "get single rows" in new WithApplication() {
      val result = await(bookRepo.getByTitle("The Rebel"))
      result.isDefined === true
      result.get.title === "The Rebel"
    }

    "insert a row" in new WithApplication() {
      await(bookRepo.insert(Book("GEB", "Hoffstadter", 2016, "orestis"))) === 1
      await(bookRepo.getAll).length === 2
    }

    "insert multiple rows" in new  WithApplication() {
      val result = bookRepo.insertAll(List(
        Book("Curry", "", 2016, "orestis"),
        Book("Howard", "", 2016, "orestis")
      ))
      await(result) === Some(2)
      await(bookRepo.getAll).length === 3
    }

    "update a row" in new  WithApplication() {
      await(
        bookRepo.update(Book("My first book ever", "Orestis Melkonian", 2016, "orestis"))
      ) === 0
    }

    "delete a row" in new  WithApplication() {
      val result = await(bookRepo.delete("The Rebel"))
      result === 1
    }
  }

  def await[T](v: Future[T]): T = Await.result(v, Duration.Inf)

}
