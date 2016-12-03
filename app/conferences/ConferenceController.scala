package conferences

import macros.Controller

import play.api.Logger
import play.api.libs.json._
import play.api.libs.ws.WSClient

import scala.concurrent.Future


@Controller
class ConferenceController {
  def info(entity: Conference, ws: WSClient): Future[JsValue] = {
    ws.url(s"https://www.TODO.com")
      .get().map { response =>
        val googleId: String = (response.json \\ "id").head.as[JsString].value
        Logger(this.getClass).info(s"Google ID: $googleId")
        JsString(s"https://books.google.fr/books?id=$googleId&printsec=frontcover&redir_esc=y")
      }
  }
}
