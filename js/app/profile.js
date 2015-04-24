define('state/profile/controller', [], function () {
    return ['$rootScope', function ($rootScope) {
		console.log('$rootScope');
    }]
});
define('profile', ['angularAMD'], function (angularAMD) {
    var app = angular.module('profile', []);

    app.config(['$stateProvider', function ($stateProvider) {
        $stateProvider
            .state('profile', {
                url: '/profile',
                views: {
                    '': angularAMD.route({
                        template: '',
                        controllerUrl: 'state/profile/controller'
                    })
                }
            })
    }]);
    return app;
});

