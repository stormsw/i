'use strict';
angular.module('dashboardJsApp')
  .controller('CreateTasksCtrl', function($scope, tasks) {

    $scope.createTask = function() {
      tasks.createTask();
      $scope.closeThisDialog();
    };
  });
