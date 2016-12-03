angular.module('mainApp',
  [
    'ngCsvImport',
    'confirmDialogBoxModule',
    'talkApp',
    'quoteApp',
    'paperApp',
    'conferenceApp',
    'bookApp',
  ]
).directive 'notification', ($timeout) ->
  restrict: 'E'
  replace: true
  scope: {ngModel: '='}
  template: '<div ng-class="ngModel.type" class="alert alert-box">{{ ngModel.content }}</div>'
  link: (scope, element, attrs) ->
    $timeout(
      () -> element.hide(),
      3000
    )

###
  Confirm dialog box module
###
angular
.module('confirmDialogBoxModule', ['ui.bootstrap'])
.directive('ngConfirmClick', [
    '$uibModal',
    ($uibModal) ->
      modalInstanceCtrl = ($scope, $uibModalInstance) ->
        $scope.ok = ->
          $uibModalInstance.close()
        $scope.cancel = ->
          $uibModalInstance.dismiss('cancel')

      restrict: 'A',
      scope: {ngConfirmClick: "&"},
      link: (scope, element, attrs) ->
        element.bind 'click', ->
          message = attrs.ngConfirmMessage || "Are you sure ?"

          # Template for confirmation dialog box
          modalHtml ='<div class="modal-body">' + message + '</div>'
          modalHtml += '<div class="modal-footer"><button class="btn btn-primary" ng-click="ok()">OK</button><button class="btn btn-default" ng-click="cancel()">Cancel</button></div>'


          modalInstance = $uibModal.open
            template: modalHtml,
            controller: modalInstanceCtrl

          modalInstance.result.then(->
            scope.ngConfirmClick()
          , -> #Modal dismissed
          )
  ]
)