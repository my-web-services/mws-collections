package papers

import macros.Controller
import play.api.libs.json._
import play.api.libs.ws.WSClient

import scala.concurrent.Future

@Controller
class PaperController {
  def info(entity: Paper, ws: WSClient): Future[JsValue] =
    Future.successful(Json.toJson(entity.link))
}
