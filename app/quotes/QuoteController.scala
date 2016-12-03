package quotes

import macros.Controller
import play.api.Logger
import play.api.libs.json._
import play.api.libs.ws.WSClient

import scala.concurrent.Future

@Controller
class QuoteController {
  def info(entity: Quote, ws: WSClient): Future[JsValue] = {
    Future.successful(Json.toJson("http://quotes.rest/qod"))
/*    ws.url("http://quotes.rest/qod.json")
      .get().map { response =>
        val quote: String = (response.json \\ "quote").head.as[JsString].value
        Logger(this.getClass).info(s"Quote of the day: $quote")
        JsString("http://quotes.rest/qod")
      }*/
  }
}
