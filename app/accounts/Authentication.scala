package accounts

import com.google.inject.{Inject, Singleton}
import accounts.model.AccountRepository
import jp.t2v.lab.play2.auth._
import accounts.model.Accounts.{accountForm, accountFormat}
import controllers.WebJarAssets
import play.api.Logger
import play.api.libs.json.{JsValue, Json}
import play.api.libs.json.Json._
import play.api.mvc.{Action, Controller}
import utils.Constants.successResponse
import main.{routes => mainRoutes}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import views.html

/**
  * Handles all requests related to authentication
  */
@Singleton
class Authentication @Inject()(val database: AccountRepository,
                               implicit val webJarAssets: WebJarAssets)
  extends Controller with AuthConfigTrait with OptionalAuthElement with LoginLogout {

  val logger = Logger(this.getClass)

  def prepareLogin() = StackAction { implicit request =>
    if (loggedIn.isDefined)
      Redirect(mainRoutes.Application.mainIndex())
    else
      Ok(html.login(accountForm))
  }

  def logout = Action.async { implicit request => gotoLogoutSucceeded }

  def login() = Action.async { implicit request =>
    accountForm.bindFromRequest.fold(
      formWithErrors => Future.successful(BadRequest(html.login(formWithErrors))),
      account => {
        database.getByName(account.name).flatMap {
          case None =>
            logger.warn(s"Wrong user")
            val form = accountForm.fill(account).withError("email", "Invalid user")
            Future.successful(BadRequest(html.login(form)))
          case Some(user) =>
            if (account.password == user.password) {
              logger.info(s"Login by ${account.name}")
              gotoLoginSucceeded(user.name)
            }
            else {
              logger.warn(s"Wrong login credentials!")
              val form = accountForm.fill(account).withError("password", "Invalid password")
              Future.successful(BadRequest(html.login(form)))
            }
        }
      }
    )
  }

  def signUp() = Action.async { implicit request =>
    accountForm.bindFromRequest.fold(
      formWithErrors => Future.successful(BadRequest(html.login(formWithErrors))),
      account => {
        database.signUp(account).map { createdAccountId =>
          Ok(html.login(accountForm))
        }
      })
  }

  def list() = Action.async {
    database.getAll.map { res =>
      logger.info("Account list: " + res)
      Ok(successResponse(Json.toJson(res), "Getting Account list successfully"))
    }
  }

}
