package accounts.model

import play.api.data.Form
import play.api.data.Forms._
import play.api.libs.json.{Json, OFormat}


case class Account(name: String, password: String)

object Accounts {

  implicit val accountFormat: OFormat[Account] = Json.format[Account]

  val accountForm = Form(
    mapping(
      "name" -> nonEmptyText,
      "password" -> nonEmptyText
    )(Account.apply)(Account.unapply)
  )

}
