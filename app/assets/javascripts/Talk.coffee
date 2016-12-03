
###
  Service
###
class TalkService

  constructor: (@$http, @$q) ->

  getAll: ->
    defer = @$q.defer()
    @$http.get('/talk/list')
    .success (res) =>
      defer.resolve(res)
    .error (err, status) => defer.reject err

    defer.promise

  delete: (title) ->
    defer = @$q.defer()
    @$http.get "/talk/delete?title=#{ title }"
    .success (res) =>
      defer.resolve res
    .error (err, status) => defer.reject err

    defer.promise

  update: (data) ->
    defer = @$q.defer()
    @$http.post '/talk/update', data
    .success (res) =>
      defer.resolve res
    .error (err, status) => defer.reject err

    defer.promise

  add: (data) ->
    defer = @$q.defer()
    @$http.post '/talk/create', data
    .success (res) =>
      defer.resolve res
    .error (err, status) => defer.reject err

    defer.promise

  addMany: (data) ->
    defer = @$q.defer()
    @$http.post '/talk/createMany', data
    .success (res) =>
      defer.resolve res
    .error (err, status) => defer.reject err

    defer.promise


###
  Controller
###
class TalkCtrl

  constructor: (@$scope, @$http, @$timeout, @$uibModal, @$window, @$parse, @TalkService) ->
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
    @TalkService.getAll().then(
      (res) => @entities = res.data
    , (err) => @showAlertMessage "error", err
    )

  editEntity: (entity) ->
    @selectedEntity = angular.copy(entity)

  updateEntity: ->
    @TalkService.update(@selectedEntity).then(
      (res) =>
        $('.modal').modal 'hide'
        @getAllEntities()
        @showAlertMessage res.status, res.msg
    , (err) => @showAlertMessage "error", err
    )

  addEntity: ->
    @TalkService.add(@newEntity).then(
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
    @TalkService.addMany(result).then(
      (res) =>
        @getAllEntities()
        @showAlertMessage res.status, res.msg
    , (err) => @showAlertMessage "error", err
    )

  deleteEntity: (title) ->
    @TalkService.delete(title).then(
      (res) =>
        newEntityList=[]
        angular.forEach @entities, (entity) =>
          newEntityList.push(entity) if entity.title != title
        @entities = newEntityList
        @showAlertMessage res.status, res.msg
    , (err) => @showAlertMessage "error", err
    )

  infoSearch: (entity) ->
    @$http.post '/talk/infoSearch', entity
    .success (res) => @$window.open res.data, '_blank'
    .error (err, status) => @showAlertMessage "error", err

###
  Module (main)
###
app = angular.module 'talkApp', ['ui.bootstrap']
             .service 'TalkService', ['$http', '$q', TalkService]
             .controller 'TalkCtrl', ['$scope', '$http', '$timeout', '$uibModal', '$window', '$parse', 'TalkService', TalkCtrl]


         