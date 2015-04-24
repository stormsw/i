define('state/documents/controller', [], function () {
    return ['$rootScope', function ($rootScope) {
		console.log('$rootScope');
    }]
});
define('documents', ['angularAMD'], function (angularAMD) {
    var app = angular.module('Documents', []);

    app.config(['$stateProvider', function ($stateProvider) {
        $stateProvider
            .state('documents', {
                url: '/documents',
                views: {
                    '': angularAMD.route({
                        template: '',
                        controllerUrl: 'state/documents/controller'
                    })
                }
            })
    }]);
    return app;
});

