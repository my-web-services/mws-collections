
###
  Service
###
class PaperService

  constructor: (@$http, @$q) ->

  getAll: ->
    defer = @$q.defer()
    @$http.get('/paper/list')
    .success (res) =>
      defer.resolve(res)
    .error (err, status) => defer.reject err

    defer.promise

  delete: (title) ->
    defer = @$q.defer()
    @$http.get "/paper/delete?title=#{ title }"
    .success (res) =>
      defer.resolve res
    .error (err, status) => defer.reject err

    defer.promise

  update: (data) ->
    defer = @$q.defer()
    @$http.post '/paper/update', data
    .success (res) =>
      defer.resolve res
    .error (err, status) => defer.reject err

    defer.promise

  add: (data) ->
    defer = @$q.defer()
    @$http.post '/paper/create', data
    .success (res) =>
      defer.resolve res
    .error (err, status) => defer.reject err

    defer.promise

  addMany: (data) ->
    defer = @$q.defer()
    @$http.post '/paper/createMany', data
    .success (res) =>
      defer.resolve res
    .error (err, status) => defer.reject err

    defer.promise


###
  Controller
###
class PaperCtrl

  constructor: (@$scope, @$http, @$timeout, @$uibModal, @$window, @$parse, @PaperService) ->
    @csv =
      content: null
      header: true
      headerVisible: true
      separator: ','
      separatorVisible: true
      result: null
      encoding: 'ISO-8859-1'
      uploadButtonLabel: "upload a csv file"
      callback: @addEntities
    @entities = []
    @alerts = []
    @selectedEntity = {}
    @newEntity = {}
    @getAllEntities()


  showAlertMessage: (status, message) ->
    switch status
      when "success" then @alerts.push(
        type: "alert-success"
        title: "SUCCESS"
        content: message
      )
      when "error" then @alerts.push(
        type: "alert-danger"
        title: "ERROR"
        content: message
      )

  getAllEntities: ->
    @PaperService.getAll().then(
      (res) => @entities = res.data
    , (err) => @showAlertMessage "error", err
    )

  editEntity: (entity) ->
    @selectedEntity = angular.copy(entity)

  updateEntity: ->
    @PaperService.update(@selectedEntity).then(
      (res) =>
        $('.modal').modal 'hide'
        @getAllEntities()
        @showAlertMessage res.status, res.msg
    , (err) => @showAlertMessage "error", err
    )

  addEntity: ->
    @PaperService.add(@newEntity).then(
      (res) =>
        $('.modal').modal 'hide'
        @getAllEntities()
        @showAlertMessage res.status, res.msg
    , (err) => @showAlertMessage "error", err
    )

  addEntities: (json) =>
    objects = @csv.result
    for obj, index in objects
      for k, v of obj
        if (+v)
          obj[k] = +v
          objects[index] = obj
    result = JSON.stringify(objects);
    @PaperService.addMany(result).then(
      (res) =>
        @getAllEntities()
        @showAlertMessage res.status, res.msg
    , (err) => @showAlertMessage "error", err
    )

  deleteEntity: (title) ->
    @PaperService.delete(title).then(
      (res) =>
        newEntityList=[]
        angular.forEach @entities, (entity) =>
          newEntityList.push(entity) if entity.title != title
        @entities = newEntityList
        @showAlertMessage res.status, res.msg
    , (err) => @showAlertMessage "error", err
    )

  infoSearch: (entity) ->
    @$http.post '/paper/infoSearch', entity
    .success (res) => @$window.open res.data, '_blank'
    .error (err, status) => @showAlertMessage "error", err

###
  Module (main)
###
app = angular.module 'paperApp', ['ui.bootstrap']
             .service 'PaperService', ['$http', '$q', PaperService]
             .controller 'PaperCtrl', ['$scope', '$http', '$timeout', '$uibModal', '$window', '$parse', 'PaperService', PaperCtrl]


         