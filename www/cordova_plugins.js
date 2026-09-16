cordova.define('cordova/plugin_list', function(require, exports, module) {
  module.exports = [
    {
      "id": "myexpenses-native.MyExpensesNative",
      "file": "plugins/myexpenses-native/www/myexpenses-native.js",
      "pluginId": "myexpenses-native",
      "clobbers": [
        "window.MyExpensesNative"
      ]
    }
  ];
  module.exports.metadata = {
    "myexpenses-native": "1.0.0"
  };
});