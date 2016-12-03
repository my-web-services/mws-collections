package macros

import scala.annotation.{StaticAnnotation, compileTimeOnly}
import scala.collection.immutable.Seq
import scala.meta._

@compileTimeOnly("@Controller not expanded")
class Controller extends StaticAnnotation {
  inline def apply(defn: Any): Any = meta {
    defn match {
      case Defn.Class(_, name, _, _, template) =>

        val main = name.value.replaceAll("Controller", "")

        // Extract class mapping from state
        val fields: Seq[(String, String)] =
          Generators.readFromFile(s"./macros_project/macros/state/$main")
            .next().split('$').map { ascribe =>
              val v :: (t :: _) = ascribe.split(':').toList
              v -> t
            }.toList
        val (primName, primType) = fields.head

        val userStat: Stat = template.stats.get.head
        val infoName = userStat match {
          case Defn.Def(_, defName, _, _, _, _) => defName
          case _ => abort("@Controller does not annotate class with a single def for information fetching.")
        }
        val primary = Term.Param(Seq(), Term.Name(primName), Some(Type.Name(primType)), None)
        val primaryName: Term.Arg = Term.Name(primary.name.value)

        // Enforce 2-step compilation
        val indexBody: Term.Arg =
          if (Generators.hasBeenGenerated(main)) {
            println(s"@$main Second compilation phase. Expansion complete.")
            arg"views.html.index(loggedIn)"
          }
          else {
            Generators.generateAll(main, fields.toList)
            println(s"@$main First compilation phase. Please re-compile for meta-dependencies.")
            arg"""successResponse(Json.toJson(0), "Dummy index")"""
          }

        val controller = q"""
           class $name @com.google.inject.Inject()
              (val repo: ${Type.Name(s"${Term.Name(s"${main.toLowerCase}s")}.${Term.Name(main)}.${main}Repository")},
               val database: accounts.model.AccountRepository,
               ws: play.api.libs.ws.WSClient,
               implicit val webJarAssets: controllers.WebJarAssets)
              extends play.api.mvc.Controller
                with accounts.AuthConfigTrait
                with jp.t2v.lab.play2.auth.AuthenticationElement {

             import play.api.libs.concurrent.Execution.Implicits.defaultContext
             import play.api._
             import play.api.mvc._
             import play.api.libs.json._
             import utils.Constants.{successResponse, failResponse}
             import scala.concurrent.Future

             val logger = play.api.Logger(this.getClass)

             def index = StackAction { implicit request =>
              Ok($indexBody)
             }

             def list() = AsyncStack { implicit request =>
               repo.getAllByUser(loggedIn.name).map { res =>
                 logger.info("List: " + res)
                 Ok(successResponse(Json.toJson(res), "Got list successfully"))
               }
             }

             def create() = AsyncStack(parse.json) { implicit request =>
               logger.info("Creating ===> " + request.body)
               _addUserToJson(loggedIn.name)(request.body).validate[${Type.Name(main)}].fold(
                  error => Future.successful(BadRequest(JsError.toJson(error))),
                 { entity =>
                     repo.insert(entity).map { createdEntityId =>
                       Ok(successResponse(Json.toJson(Map("id" ->createdEntityId)),
                                          ${Lit(main)} + " has been created successfully."))
                     }
                 }
               )
             }

             def createMany() = AsyncStack(parse.json) { implicit request =>
               logger.info("Creating many ===> " + request.body)
               _addUserToJsons(loggedIn.name)(request.body.as[JsArray]).validate[List[${Type.Name(main)}]].fold(
                 error => Future.successful(BadRequest(JsError.toJson(error)))
               , { entities =>
                     repo.insertAll(entities).map { insertedNumber =>
                       Ok(successResponse(Json.toJson(Map("inserted" -> insertedNumber)),
                         s"$${insertedNumber.get} " + ${Lit(main)} + "(s) have been created successfully."))
                     }
                 }
               )
             }

             def delete($primary) = AsyncStack { request =>
               repo.delete($primaryName).map { _ =>
                 Ok(successResponse(Json.toJson("{}"), ${Lit(main)} + " has been deleted successfully."))
               }
             }

             def edit($primary) = AsyncStack { request =>
               repo.getById($primaryName).map { entityOpt =>
                 entityOpt.fold(Ok(failResponse(Json.obj(), ${Lit(main)} + " does not exist.")))(entity => Ok(
                   successResponse(Json.toJson(entity), "Got " + ${Lit(main)} + " successfully")))
               }
             }

             def update = AsyncStack(parse.json) { implicit request =>
               logger.info("Updating ===> " + request.body)
               _addUserToJson(loggedIn.name)(request.body).validate[${Type.Name(main)}].fold(
                 error => Future.successful(BadRequest(JsError.toJson(error))),
                 { entity =>
                     repo.update(entity).map { res =>
                       Ok(successResponse(Json.toJson("{}"), ${Lit(main)} + " has been updated successfully."))
                     }
                 }
               )
             }

             def infoSearch = AsyncStack(parse.json) { implicit request =>
               logger.info("Searching for ===> " + request.body)
               _addUserToJson(loggedIn.name)(request.body).validate[${Type.Name(main)}].fold(
                 error => Future.successful(BadRequest(JsError.toJson(error))),
                 entity => $infoName(entity, ws).map { information =>
                   Ok(successResponse(information, "Information has been fetched successfully"))
                 }
               )
             }

             def _addUserToJson(userName: String)(data: JsValue): JsValue =
               data.as[JsObject] + ("accountId" -> Json.toJson(userName))

             def _addUserToJsons(username: String)(data: JsArray): JsArray =
               JsArray(data.value.map(_addUserToJson(username)))
           }
         """
        val combinedStats: Seq[Stat] = controller.templ.stats.get ++ template.stats.get
        val complete = controller.copy(templ = controller.templ.copy(stats = Some(combinedStats)))
        complete
      case _ =>
        abort("@Controller must annotate a class.")
    }
  }
}
