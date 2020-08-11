# cordova-globostore-autoupdate

A library which provides auto-update feature to any hibrid (Android and IOS) cordova application from [globostore](https://globostore.apps.tvglobo.com.br/).

## Installation

Add the plugins to your cordova project:
```shell
$ cordova plugin add globostore-autoupdate-plugin
$ cordova plugin add cordova-plugin-androidx
$ cordova plugin add cordova-plugin-androidx-adapter
```

In config.xml add this lines inside parent widget:
```xml
    <preference name="appId" value="<< your_app_id >>" />
    <preference name="appKey" value="<< your_app_key >>" />
    <preference name="versionUrl" value="https://services.apps.tvglobo.com.br/rpc" />
    <preference name="method" value="CrudService.GetCurrentAppVersion" />
    <preference name="service" value="crud" />
    <preference name="downloadUrl" value="https://download.globostore.apps.tvglobo.com.br" />
```
Where `app_id` and `app_key` is provided by [globostore](https://globostore.apps.tvglobo.com.br/) to your application when it is registered.

Run the auto-update feature when your application is loaded, 
for example, in index.html:
```html
    <script type="text/javascript">
        function onLoad() {
            document.addEventListener('deviceready', onDeviceReady, false);
        }

        function onDeviceReady() {
            window.GlobostoreAutoUpdate.check('version', function () {})
        }
    </script>

    <body onload="onLoad()">
        <-- your project src code -->
    </body>
```
