'use strict';

var should = require('should');
var appTest = require('../app.spec');

describe('test eds callback', function () {
  it('should create cookie session', function (done) {
    appTest.loginWithEds(function (error) {
      if (error) {
        done(error)
      } else {
        done();
      }
    });
  });
});
