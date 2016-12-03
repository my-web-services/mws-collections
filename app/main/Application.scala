package main

import accounts.AuthConfigTrait
import accounts.model.AccountRepository
import com.google.inject.Inject
import controllers.WebJarAssets
import jp.t2v.lab.play2.auth.AuthenticationElement
import play.api.mvc._
import views.html

class Application @Inject()(val database: AccountRepository,
                            implicit val webJarAssets: WebJarAssets)
  extends Controller with AuthConfigTrait with AuthenticationElement {

  def mainIndex = StackAction { implicit request =>
    Ok(html.mainIndex(loggedIn))
  }

}
