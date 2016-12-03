package books

import macros.Controller
import play.api.Logger

import scala.concurrent.Future
import play.api.libs.json._
import play.api.libs.ws.WSClient

@Controller
class BookController {
  def info(entity: Book, ws: WSClient): Future[JsValue] = {
    ws.url(s"https://www.googleapis.com/books/v1/volumes?q=intitle:${entity.title}+inauthor:${entity.author}&key=AIzaSyAP_-Rb-Hiw1C_fvOjzPBqLqttuJ-bspMA")
      .get().map { response =>
        val googleId: String = (response.json \\ "id").head.as[JsString].value
        Logger(this.getClass).info(s"Google ID: $googleId")
        JsString(s"https://books.google.fr/books?id=$googleId&printsec=frontcover&redir_esc=y")
      }
  }
}
