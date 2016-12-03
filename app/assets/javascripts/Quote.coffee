
###
  Service
###
class QuoteService

  constructor: (@$http, @$q) ->

  getAll: ->
    defer = @$q.defer()
    @$http.get('/quote/list')
    .success (res) =>
      defer.resolve(res)
    .error (err, status) => defer.reject err

    defer.promise

  delete: (quote) ->
    defer = @$q.defer()
    @$http.get "/quote/delete?quote=#{ quote }"
    .success (res) =>
      defer.resolve res
    .error (err, status) => defer.reject err

    defer.promise

  update: (data) ->
    defer = @$q.defer()
    @$http.post '/quote/update', data
    .success (res) =>
      defer.resolve res
    .error (err, status) => defer.reject err

    defer.promise

  add: (data) ->
    defer = @$q.defer()
    @$http.post '/quote/create', data
    .success (res) =>
      defer.resolve res
    .error (err, status) => defer.reject err

    defer.promise

  addMany: (data) ->
    defer = @$q.defer()
    @$http.post '/quote/createMany', data
    .success (res) =>
      defer.resolve res
    .error (err, status) => defer.reject err

    defer.promise


###
  Controller
###
class QuoteCtrl

  constructor: (@$scope, @$http, @$timeout, @$uibModal, @$window, @$parse, @QuoteService) ->
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
    @QuoteService.getAll().then(
      (res) => @entities = res.data
    , (err) => @showAlertMessage "error", err
    )

  editEntity: (entity) ->
    @selectedEntity = angular.copy(entity)

  updateEntity: ->
    @QuoteService.update(@selectedEntity).then(
      (res) =>
        $('.modal').modal 'hide'
        @getAllEntities()
        @showAlertMessage res.status, res.msg
    , (err) => @showAlertMessage "error", err
    )

  addEntity: ->
    @QuoteService.add(@newEntity).then(
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
    @QuoteService.addMany(result).then(
      (res) =>
        @getAllEntities()
        @showAlertMessage res.status, res.msg
    , (err) => @showAlertMessage "error", err
    )

  deleteEntity: (quote) ->
    @QuoteService.delete(quote).then(
      (res) =>
        newEntityList=[]
        angular.forEach @entities, (entity) =>
          newEntityList.push(entity) if entity.quote != quote
        @entities = newEntityList
        @showAlertMessage res.status, res.msg
    , (err) => @showAlertMessage "error", err
    )

  infoSearch: (entity) ->
    @$http.post '/quote/infoSearch', entity
    .success (res) => @$window.open res.data, '_blank'
    .error (err, status) => @showAlertMessage "error", err

###
  Module (main)
###
app = angular.module 'quoteApp', ['ui.bootstrap']
             .service 'QuoteService', ['$http', '$q', QuoteService]
             .controller 'QuoteCtrl', ['$scope', '$http', '$timeout', '$uibModal', '$window', '$parse', 'QuoteService', QuoteCtrl]


         