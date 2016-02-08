'use strict';

// Development specific configuration
// ==================================
module.exports = {

  debug: true,

  bankid: {
    sProtocol_AccessService_BankID: 'https', //Test
    sHost_AccessService_BankID: 'bankid.privatbank.ua', //Test
    sProtocol_ResourceService_BankID: 'https', //Test
    sHost_ResourceService_BankID: 'bankid.privatbank.ua', //Test
    client_id: 'testIgov',//testIgovEncrypt use to force test bank id  returns encrypted data
    client_secret: 'testIgovSecret',
    privateKey: (__dirname + '/iGov_sgn.pem'),
    privateKeyPassphrase: '1234567899'
  },

  soccard: {
    socCardAPIProtocol: 'https',
    socCardAPIHostname: 'test.kyivcard.com.ua',
    socCardAPIVersion: '1.0'
  },

  server: {
    protocol: 'http',
    port: '9000',
    session: {
      secret: 'put yor session secret here',
      key: ['solt for session 1', 'solt for session 2'],
      secure: false,
      maxAge: 14400000 // 4h*60m*60s*1000ms*/
    }
  },

  activiti: {
    protocol: 'https',
    hostname: 'test.igov.org.ua',
    port: '8443',
    path: '/wf/service',
    username: 'activiti-master',
    password: 'UjhtJnEvf!'
  }
};
