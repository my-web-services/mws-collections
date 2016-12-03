package macros

import java.io._
import scala.io._
import sys.process._

object Generators {

  var main: String = ""
  var fieldsWithTypes: Seq[(String, String)] = Seq()

  def hasBeenGenerated(main: String): Boolean =
    readFromFile("./conf/routes").contains(s"# $main-related routes")

  def cleanup(): Unit =
    println("Running clean_gen......" + (if ("./clean_gen.sh".! == 0) "Done" else "Failed"))

  def generateAll(main: String, fieldsWithTypes: Seq[(String, String)]): Unit = {
    this.main = main
    this.fieldsWithTypes = fieldsWithTypes
//    cleanup()
    updateSqlEvolutionScript()
    updateRoutes()
    updateMainHtml()
    updateIndexHtml()
    updateAngularModule()
    generateJS()
    generateHTML()
  }

  def updateIndexHtml(): Unit = {
    val mainL = main.toLowerCase
    val filename = "./app/main/views/mainIndex.scala.html"
    val toDetect = """    <ul id="link_list">"""
    val toInsert = s"""      <a href="@${mainL}s.routes.${main}Controller.index()">$main</a>"""
    changeLine(
      filename,
      toDetect,
      Seq(toDetect, toInsert)
    )
  }

  def updateMainHtml(): Unit = {
    val filename = "./app/main/views/_template.scala.html"
    val toDetect = "         <!-- Custom Javascripts Links -->"
    val toInsert = s"""        <script type="text/javascript" src="@routes.Assets.at("javascripts/$main.js")"></script>"""
    changeLine(
      filename,
      toDetect,
      Seq(toDetect, toInsert)
    )
  }

  def updateSqlEvolutionScript(): Unit = {
    val mainL = main.toLowerCase
    val filename = "./conf/evolutions/default/1.sql"
    val toDetect = "# --- !Downs"

    // Table create
    val primName = fieldsWithTypes.head._1
    val fields: Seq[String] = fieldsWithTypes.map { case (f, t) =>
      s"""   "$f" ${toSqlType(t)}""" + (f match {
        case `primName` => " PRIMARY KEY,"
        case _ => ","
      })
    } :+ """   "accountId" VARCHAR""" :+ ");"

    val up: Seq[String] = Seq(s"""CREATE TABLE "$mainL" (""") ++ fields

    // Dummy record
    val defaultValues = fieldsWithTypes.map(_._2).map(toDummyValue).mkString(", ")
    val dummy = s"""INSERT INTO "$mainL" values ($defaultValues, 'orestis');"""

    // Table drop
    val down = s"""DROP TABLE "${main.toLowerCase}";"""

    // Write to file
    changeLine(
      filename,
      toDetect,
      up :+ dummy :+ "" :+ toDetect :+ "" :+ down
    )
  }

  def updateAngularModule(): Unit = {
    val filename = "./app/assets/javascripts/app.coffee"
    val toDetect = "    'confirmDialogBoxModule',"
    changeLine(
      filename,
      toDetect,
      Seq(toDetect, s"    '${main.toLowerCase}App',")
    )
  }

  def updateRoutes(): Unit = {
    val (primName, primType) = fieldsWithTypes.head
    val mainL = main.toLowerCase
    val controller = s"${mainL}s.${main}Controller"
    appendToFile(
      "./conf/routes",
      s"""
         |# $main-related routes
         |GET     /${mainL}s               $controller.index
         |GET     /$mainL/list             $controller.list
         |POST    /$mainL/create           $controller.create
         |POST    /$mainL/createMany       $controller.createMany
         |GET     /$mainL/edit             $controller.edit($primName: $primType)
         |POST    /$mainL/update           $controller.update
         |GET     /$mainL/delete           $controller.delete($primName: $primType)
         |POST    /$mainL/infoSearch       $controller.infoSearch
         |
       """.stripMargin
    )
  }

  def generateHTML(): Unit = {
    val mainL = main.toLowerCase
    val plural = s"${mainL}s"
    val fields = fieldsWithTypes.map(_._1)
    val primary = fields.head

    // Index page
    writeToFile(
      s"./app/$plural/views/index.scala.html",
      s"""
         |@import accounts.model.Account
         |@import $plural.views.html
         |@import main.views.{html => mainHtml}
         |
         |@(account: Account)(implicit webJarAssets: WebJarAssets)
         |
         |@mainHtml._template(s"$${account.name}'s $mainL database", fav = "$plural")(webJarAssets) {
         |
         |<div ng-controller="${main}Ctrl as ctrl">
         |
         |    <div>
         |        <div class="row entity-header">
         |            <!-- $main search -->
         |            <div class="col-sm-4 col-md-4 col-lg-4">
         |                <input class="form-control"
         |                       type="text"
         |                       ng-model="search$main" placeholder="Search $mainL ..."/>
         |            </div>
         |            <!-- Alert messages -->
         |            <div class="col-sm-4 col-md-4 col-lg-4">
         |                <div ng-repeat="alert in ctrl.alerts">
         |                <notification ng-model="alert"></notification>
         |                </div>
         |            </div>
         |            <!-- Add new $mainL link -->
         |            <div class="col-sm-4 col-md-4 col-lg-4">
         |                <button class="btn btn-success btn-sm add-button" data-toggle="modal" data-target="#new${main}Modal">Add New $main</button>
         |            </div>
         |
         |        </div>
         |     </div>
         |    <hr>
         |
         |    @html.list(account)
         |    @html.newForm(account)
         |    @html.editForm(account)
         |    @html.upload()
         |
         |</div>
         |}
       """.stripMargin
    )

    // List page
    writeToFile(
      s"./app/$plural/views/list.scala.html",
      s"""
         |@import accounts.model.Account
         |
         |@(account: Account)
         |<table class="table table-hover">
         |    <thead>
         |    <tr>
         |        ${fields.map {f => s"<th>${f.capitalize}</th>"}.reduce(_++_) }
         |    </tr>
         |    </thead>
         |    <tbody>
         |    <tr ng-repeat="entity in ctrl.entities | filter: search$main">
         |        ${fields.map {f => s"<td>{{entity.$f}}</td>"}.reduce(_++_) }
         |        <td>
         |            <i title="Edit" data-toggle="modal" data-target="#edit${main}Modal" class="glyphicon glyphicon-edit cursorPointer edit-button" ng-click="ctrl.editEntity(entity)">
         |            </i>
         |        </td>
         |        <td>
         |            <i title="Delete" class="glyphicon glyphicon-trash cursorPointer delete-button" ng-confirm-message="Are you sure want to delete?" ng-confirm-click="ctrl.deleteEntity(entity.$primary)">
         |            </i>
         |        </td>
         |        <td>
         |            <i title="Google Search" class="glyphicon glyphicon-book cursorPointer google-button" ng-click="ctrl.infoSearch(entity)">
         |            </i>
         |        </td>
         |    </tr>
         |    </tbody>
         |</table>
         |
         |<p>Showing <strong>{{ (ctrl.entities | filter: search$main).length }}</strong> of <strong>{{ ctrl.entities.length }}</strong> entries</p>
       """.stripMargin
    )

    // New form
    val newFormFields = fieldsWithTypes.map { case (f, t) =>
      val capitalized = f.capitalize
      s"""
         |<div class="form-group">
         |  <label for="entity$capitalized">$capitalized</label>
         |  <input type="${toFormType(t)}"
         |         class="form-control"
         |         id="entity$capitalized"
         |         name="$f"
         |         ng-model="ctrl.newEntity.$f"
         |         placeholder="$capitalized"
         |         required>
         |</div>
       """.stripMargin.replace('\n', ' ')
    }.reduce(_++_)

    writeToFile(
      s"./app/$plural/views/newForm.scala.html",
      s"""
         |@import accounts.model.Account
         |
         |@(account: Account)
         |<!-- $main Form Modal -->
         |<form id="new${main}Form" role="form" ng-submit="ctrl.addEntity()">
         |    <div class="modal fade" id="new${main}Modal" tabindex="-1" role="dialog" aria-labelledby="myModalLabel" >
         |        <div class="modal-dialog" role="document">
         |            <div class="modal-content">
         |                <div class="modal-header">
         |                    <button type="button" class="close" data-dismiss="modal" aria-label="Close"><span aria-hidden="true">&times;</span></button>
         |                    <h4 class="modal-title" id="myModalLabel">Fill $main Details</h4>
         |                </div>
         |                <div class="modal-body">
         |                    $newFormFields
         |                </div>
         |                <div class="modal-footer">
         |                    <button type="button" class="btn btn-default" data-dismiss="modal" >Cancel</button>
         |                    <input type="submit" ng-disabled="new${main}Form.$$invalid" class="btn btn-warning" id="submit" value="Add New" />
         |                </div>
         |            </div>
         |        </div>
         |    </div>
         |</form>
       """.stripMargin
    )

    // Edit form
    val editFormFields = fieldsWithTypes.map { case (f, t) =>
      s"""
         |<div class="form-group">
         |  <label for="$f">${f.capitalize}</label>
         |  <input type="${toFormType(t)}"
         |         class="form-control"
         |         ng-model="ctrl.selectedEntity.$f"
         |         id="$f"
         |         value="{{ctrl.selectedEntity.$f}}"
         |         required>
         |</div>
       """.stripMargin.replace('\n', ' ')
    }.reduce(_++_)

    writeToFile(
      s"./app/$plural/views/editForm.scala.html",
      s"""
         |@import accounts.model.Account
         |
         |@(account: Account)
         |<!-- $main Edit Form Modal -->
         |<form name="edit${main}Form" ng-submit="ctrl.updateEntity()">
         |    <div class="modal fade" id="edit${main}Modal" tabindex="-1" role="dialog" aria-labelledby="myModalLabel" >
         |        <div class="modal-dialog" role="document">
         |            <div class="modal-content">
         |                <div class="modal-header">
         |                    <button type="button" class="close" data-dismiss="modal" aria-label="Close"><span aria-hidden="true">&times;</span></button>
         |                    <h4 class="modal-title" id="myModalLabel">Edit $main</h4>
         |                </div>
         |                <div class="modal-body">
         |                    $editFormFields
         |                </div>
         |                <div class="modal-footer">
         |                    <button type="button" class="btn btn-default" data-dismiss="modal" >Cancel</button>
         |                    <input type="submit" ng-disabled="edit${main}Form.$$invalid" class="btn btn-warning" value="Save" />
         |                </div>
         |            </div>
         |        </div>
         |    </div>
         |</form>
       """.stripMargin
    )

    // CSV form
    writeToFile(
      s"./app/$plural/views/upload.scala.html",
      s"""
         |<ng-csv-import
         |    content="ctrl.csv.content"
         |    header="ctrl.csv.header"
         |    separator="ctrl.csv.separator"
         |    result="ctrl.csv.result"
         |    upload-button-label="ctrl.csv.uploadButtonLabel"
         |    callback="ctrl.csv.callback"
         |></ng-csv-import>
       """.stripMargin
    )

  }

  def generateJS(): Unit = {
    val primaryName = fieldsWithTypes.head._1
    val mainL = main.toLowerCase
    val (service, ctrl, app) = (s"${main}Service", s"${main}Ctrl", s"${mainL}App")
    writeToFile(
      s"./app/assets/javascripts/$main.coffee",
      s"""
         |###
         |  Service
         |###
         |class $service
         |
         |  constructor: (@$$http, @$$q) ->
         |
         |  getAll: ->
         |    defer = @$$q.defer()
         |    @$$http.get('/$mainL/list')
         |    .success (res) =>
         |      defer.resolve(res)
         |    .error (err, status) => defer.reject err
         |
         |    defer.promise
         |
         |  delete: ($primaryName) ->
         |    defer = @$$q.defer()
         |    @$$http.get "/$mainL/delete?$primaryName=#{ $primaryName }"
         |    .success (res) =>
         |      defer.resolve res
         |    .error (err, status) => defer.reject err
         |
         |    defer.promise
         |
         |  update: (data) ->
         |    defer = @$$q.defer()
         |    @$$http.post '/$mainL/update', data
         |    .success (res) =>
         |      defer.resolve res
         |    .error (err, status) => defer.reject err
         |
         |    defer.promise
         |
         |  add: (data) ->
         |    defer = @$$q.defer()
         |    @$$http.post '/$mainL/create', data
         |    .success (res) =>
         |      defer.resolve res
         |    .error (err, status) => defer.reject err
         |
         |    defer.promise
         |
         |  addMany: (data) ->
         |    defer = @$$q.defer()
         |    @$$http.post '/$mainL/createMany', data
         |    .success (res) =>
         |      defer.resolve res
         |    .error (err, status) => defer.reject err
         |
         |    defer.promise
         |
         |
         |###
         |  Controller
         |###
         |class $ctrl
         |
         |  constructor: (@$$scope, @$$http, @$$timeout, @$$uibModal, @$$window, @$$parse, @$service) ->
         |    @csv =
         |      content: null
         |      header: true
         |      headerVisible: true
         |      separator: ','
         |      separatorVisible: true
         |      result: null
         |      encoding: 'ISO-8859-1'
         |      uploadButtonLabel: "upload a csv file"
         |      callback: @addEntities
         |    @entities = []
         |    @alerts = []
         |    @selectedEntity = {}
         |    @newEntity = {}
         |    @getAllEntities()
         |
         |
         |  showAlertMessage: (status, message) ->
         |    switch status
         |      when "success" then @alerts.push(
         |        type: "alert-success"
         |        title: "SUCCESS"
         |        content: message
         |      )
         |      when "error" then @alerts.push(
         |        type: "alert-danger"
         |        title: "ERROR"
         |        content: message
         |      )
         |
         |  getAllEntities: ->
         |    @$service.getAll().then(
         |      (res) => @entities = res.data
         |    , (err) => @showAlertMessage "error", err
         |    )
         |
         |  editEntity: (entity) ->
         |    @selectedEntity = angular.copy(entity)
         |
         |  updateEntity: ->
         |    @$service.update(@selectedEntity).then(
         |      (res) =>
         |        $$('.modal').modal 'hide'
         |        @getAllEntities()
         |        @showAlertMessage res.status, res.msg
         |    , (err) => @showAlertMessage "error", err
         |    )
         |
         |  addEntity: ->
         |    @$service.add(@newEntity).then(
         |      (res) =>
         |        $$('.modal').modal 'hide'
         |        @getAllEntities()
         |        @showAlertMessage res.status, res.msg
         |    , (err) => @showAlertMessage "error", err
         |    )
         |
         |  addEntities: (json) =>
         |    objects = @csv.result
         |    for obj, index in objects
         |      for k, v of obj
         |        if (+v)
         |          obj[k] = +v
         |          objects[index] = obj
         |    result = JSON.stringify(objects);
         |    @$service.addMany(result).then(
         |      (res) =>
         |        @getAllEntities()
         |        @showAlertMessage res.status, res.msg
         |    , (err) => @showAlertMessage "error", err
         |    )
         |
         |  deleteEntity: ($primaryName) ->
         |    @$service.delete($primaryName).then(
         |      (res) =>
         |        newEntityList=[]
         |        angular.forEach @entities, (entity) =>
         |          newEntityList.push(entity) if entity.$primaryName != $primaryName
         |        @entities = newEntityList
         |        @showAlertMessage res.status, res.msg
         |    , (err) => @showAlertMessage "error", err
         |    )
         |
         |  infoSearch: (entity) ->
         |    @$$http.post '/$mainL/infoSearch', entity
         |    .success (res) => @$$window.open res.data, '_blank'
         |    .error (err, status) => @showAlertMessage "error", err
         |
         |###
         |  Module (main)
         |###
         |app = angular.module '$app', ['ui.bootstrap']
         |             .service '$service', ['$$http', '$$q', $service]
         |             .controller '$ctrl', ['$$scope', '$$http', '$$timeout', '$$uibModal', '$$window', '$$parse', '$service', $ctrl]
         |
         |
         """.stripMargin
    )
  }

  def readFromFile(filename: String): Iterator[String] =
    Source.fromFile(filename).getLines()

  def writeToFile(filename: String, content: String): Unit = {
    val file = new File(filename)
    file.getParentFile.mkdirs()
    file.createNewFile()
    val writer = new PrintWriter(file)
    writer.print(content)
    writer.close()
    println("Wrote to " + filename)
  }

  def appendToFile(filename: String, content: String): Unit = {
    new PrintWriter(new FileOutputStream(new File(filename), true)) { append(content) ; close() }
    println("Appended to " + filename)
  }

  def changeLine(filename: String, toDetect: String, toInsert: Seq[String]): Unit = {
    writeToFile(
      filename,
      readFromFile(filename).flatMap {
        case `toDetect` => toInsert
        case line => Seq(line)
      }.mkString("\n")
    )
  }

  private def toFormType(scalaType: String) = scalaType match {
    case "String" => "text"
    case "Int" => "number"
  }

  private def toSqlType(scalaType: String) = scalaType match {
    case "String" => "VARCHAR"
    case "Int" => "INTEGER"
  }

  private def toDummyValue(scalaType: String) = scalaType match {
    case "String" => """'dummy'"""
    case "Int" => "666"
  }

}
