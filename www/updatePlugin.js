var exec = require('cordova/exec');

function InAppUpdate() {}

// The function that passes work along to native shells
// Message is a string, duration may be 'long' or 'short'
InAppUpdate.prototype.update = function(successCallback, errorCallback, config) {
  exec(successCallback, errorCallback, 'UpdatePlugin', 'update', [{ 'flexibleUpdateStalenessDays': 2, 'immediateUpdateStalenessDays': 5, 'type': 'IMMEDIATE' }]);
}

InAppUpdate.prototype.check = function(successCallback, errorCallback) {
  exec(successCallback, errorCallback, 'UpdatePlugin', 'check');
}

// Installation constructor that binds updatePlugin to window
 InAppUpdate.install = function() {
   if (!window.plugins) {
     window.plugins = {};
   }
   window.plugins.InAppUpdate = new InAppUpdate();
  return window.plugins.InAppUpdate;
 };

 cordova.addConstructor(InAppUpdate.install);
