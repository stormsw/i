requirejs.config({
    waitSeconds: 30,
    baseUrl: './js',
    paths: {
        'bower': './bower',
		'angularAMD': './angularAMD/angularAMD',
		'ngload': './angularAMD/ngload',
		'script': './script',
		'index': './app/index',
		'documents': './app/documents',
		'journal': './app/journal',
		'profile': './app/profile',
    },
    shim: {
        'angularAMD': ['bower'],
		'ngload': ['bower'],
        'script': ['angularAMD', 'ngload']
    },
    deps: ['script']
});