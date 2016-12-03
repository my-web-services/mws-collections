package utils

import play.api.libs.json.{JsObject, JsValue}
import play.api.libs.json.Json._


object Constants {

  def response(status: String)(data: JsValue, message: String): JsObject =
    obj("status" -> status, "data" -> data, "msg" -> message)

  def successResponse(data: JsValue, message: String) = response("success")(data, message)

  def failResponse(data: JsValue, message: String) = response("error")(data, message)

}
