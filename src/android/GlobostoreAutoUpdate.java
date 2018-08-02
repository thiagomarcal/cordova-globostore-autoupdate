package br.com.globostore.autoupdate;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.cordova.CordovaWebView;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;
import org.apache.cordova.CordovaInterface;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;

import org.json.JSONArray;
import org.json.JSONException;
import org.xmlpull.v1.XmlPullParserException;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.util.Log;

import android.widget.TextView;

public class GlobostoreAutoUpdate extends CordovaPlugin {
    public static final String TAG = "GlobostoreAutoUpdate";

    private List<String> supportedKeys = new ArrayList(Arrays.asList("appId", "appKey", "versionUrl", "method", "service", "downloadUrl"));
    private Map mapa = null;


    /**
     * Constructor.
     */
    public GlobostoreAutoUpdate() {
    }

    /**
     * Sets the context of the Command. This can then be used to do things like
     * get file paths associated with the Activity.
     *
     * @param cordova The context of the main Activity.
     * @param webView The CordovaWebView Cordova is running in.
     */
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);
        Log.d(TAG, "Initializing GlobostoreAutoUpdate");
    }

    /**
     * Executes the request and returns PluginResult.
     *
     * @param action          The action to execute.
     * @param args            JSONArry of arguments for the plugin.
     * @param callbackContext The callback id used when calling back into JavaScript.
     * @return True if the action was valid, false if not.
     */
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {

        //Context ctx = cordova.getContext();
        Context ctx = this.cordova.getActivity().getApplicationContext();
        int identifier = ctx.getResources().getIdentifier("config", "xml", ctx.getPackageName());
        mapa = loadConfigsFromXml(ctx.getResources(), identifier);

        if (this.cordova.getActivity().isFinishing()) return true;
        if ("check".equals(action)) {
            Version remote = checkRemoteVersion();

            if (!remote.getVersion().equals(checkCurrentVersion(ctx))) {
                this.alert("Atualização", "Nova versão disponível", "OK", callbackContext, remote);
            }
        }

        return true;
    }

    /**
     * Builds and shows a native Android alert with given Strings
     *
     * @param message         The message the alert should display
     * @param title           The title of the alert
     * @param buttonLabel     The label of the button
     * @param callbackContext The callback context
     */
    public synchronized void alert(final String message, final String title, final String buttonLabel, final CallbackContext callbackContext, final Version version) {
        final CordovaInterface cordova = this.cordova;

        Runnable runnable = new Runnable() {
            public void run() {

                AlertDialog.Builder dlg = createDialog(cordova); // new AlertDialog.Builder(cordova.getActivity(), AlertDialog.THEME_DEVICE_DEFAULT_LIGHT);
                dlg.setMessage(message);
                dlg.setTitle(title);
                dlg.setCancelable(true);
                dlg.setPositiveButton(buttonLabel,
                        new AlertDialog.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                                callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK, 0));
                                UpdateApp update = new UpdateApp();
                                update.setContext(cordova.getActivity().getApplicationContext());
                                update.setCallback(callbackContext);
                                update.execute("https://download.globostore.apps.tvglobo.com.br/" + version.getDownloadUrl());
                            }
                        });
                dlg.setOnCancelListener(new AlertDialog.OnCancelListener() {
                    public void onCancel(DialogInterface dialog) {
                        dialog.dismiss();
                        callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK, 0));
                    }
                });

                changeTextDirection(dlg);
            }

            ;
        };
        this.cordova.getActivity().runOnUiThread(runnable);
    }

    @SuppressLint("NewApi")
    private AlertDialog.Builder createDialog(CordovaInterface cordova) {
        int currentapiVersion = android.os.Build.VERSION.SDK_INT;
        if (currentapiVersion >= android.os.Build.VERSION_CODES.HONEYCOMB) {
            return new AlertDialog.Builder(cordova.getActivity(), AlertDialog.THEME_DEVICE_DEFAULT_LIGHT);
        } else {
            return new AlertDialog.Builder(cordova.getActivity());
        }
    }

    @SuppressLint("NewApi")
    private void changeTextDirection(Builder dlg) {
        int currentapiVersion = android.os.Build.VERSION.SDK_INT;
        dlg.create();
        AlertDialog dialog = dlg.show();
        if (currentapiVersion >= android.os.Build.VERSION_CODES.JELLY_BEAN_MR1) {
            TextView messageview = (TextView) dialog.findViewById(android.R.id.message);
            messageview.setTextDirection(android.view.View.TEXT_DIRECTION_LOCALE);
        }
    }

    private Version checkRemoteVersion() {
        Version version = new Version();
        try {
            RequestVersion rv = new RequestVersion();
            rv.setMap(mapa);
            version = rv.execute(mapa.get("versionUrl").toString()).get();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return version;

    }

    private String checkCurrentVersion(Context ctx) {

        String currentVersion = null;

        try {
            PackageInfo pInfo = ctx.getPackageManager().getPackageInfo(ctx.getPackageName(), 0);
            currentVersion = pInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        return currentVersion;
    }


    private Map loadConfigsFromXml(Resources res, int configXmlResourceId) {
        //
        // Resources is the same thing from above that can be obtained
        // by context.getResources()
        // configXmlResourceId is the resource id integer obtained in step 1
        XmlResourceParser xrp = res.getXml(configXmlResourceId);

        Map configs = new HashMap();

        //
        // walk the config.xml tree and save all <preference> tags we want
        //
        try {
            xrp.next();
            while (xrp.getEventType() != XmlResourceParser.END_DOCUMENT) {
                if ("preference".equals(xrp.getName())) {
                    String key = matchSupportedKeyName(xrp.getAttributeValue(null, "name"));
                    if (key != null) {
                        configs.put(key, xrp.getAttributeValue(null, "value"));
                    }
                }
                xrp.next();
            }
        } catch (XmlPullParserException ex) {
            Log.e(TAG, ex.toString());
        } catch (IOException ex) {
            Log.e(TAG, ex.toString());
        }

        return configs;
    }

    private String matchSupportedKeyName(String testKey) {
        //
        // If key matches, return the version with correct casing.
        // If not, return null.
        // O(n) here is okay because this is a short list of just a few items
        for (String realKey : supportedKeys) {
            if (realKey.equalsIgnoreCase(testKey)) {
                return realKey;
            }
        }
        return null;
    }


}
