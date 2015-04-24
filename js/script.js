define(['angularAMD'], function (angularAMD) {
    var app = angular.module("main", ['ct.ui.router.extras']);

    app.config(['$futureStateProvider', '$controllerProvider', function ($futureStateProvider, $controllerProvider) {
        var ngloadStateFactory = ['$q', 'futureState', function ($q, futureState) {
            var ngloadDeferred = $q.defer();
            require(["ngload!" + futureState.src, 'ngload', 'angularAMD'], function ngloadCallback(result, ngload, angularAMD) {
                angularAMD.processQueue();
                ngloadDeferred.resolve(undefined);
            });
            return ngloadDeferred.promise;
        }];
        // Loading states from .json file during runtime
        var loadAndRegisterFutureStates = ['$http', function ($http) {
            // $http.get().then() returns a promise
            return $http.get('./data.json').then(function (response) {
                angular.forEach(response.data, function (futureState) {
                    // Register each state returned from $http.get() with $futureStateProvider
                    $futureStateProvider.futureState(futureState);
                });
            });
        }];
        $futureStateProvider.stateFactory('ngload', ngloadStateFactory); // register AngularAMD ngload state factory
        $futureStateProvider.addResolve(loadAndRegisterFutureStates);
    }]);

    angularAMD.bootstrap(app);
    return app;
});