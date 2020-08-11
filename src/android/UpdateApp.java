package br.com.globostore.autoupdate;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.StrictMode;
import android.support.v4.content.FileProvider;
import android.util.Log;

import org.apache.cordova.CallbackContext;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URL;

public class UpdateApp extends AsyncTask<String, Void, Void> {
    private Context context;
    private Handler handler;

    private CallbackContext callbackContext;

    public void setContext(Context context) {
        this.context = context;
    }

    public void setCallback(CallbackContext callbackContext) {
        this.callbackContext = callbackContext;
    }

    public void setHandler(Handler handler) {
        this.handler = handler;
    }

    @Override
    protected Void doInBackground(String... arg0) {
        String apkurl = arg0[0];
        String outputFileName = "install.apk";
        checkFileUriExposure();
        try {
            URL url = new URL(apkurl);
            HttpURLConnection c = (HttpURLConnection) url.openConnection();
            c.setRequestMethod("GET");
            c.connect();

            int fileLength = c.getContentLength();
            long total = 0;

            String PATH = getPath(context);
            File file = new File(PATH);
            file.mkdirs();

            File outputFile = new File(file, outputFileName);
            outputFile.createNewFile();
            FileOutputStream fos = new FileOutputStream(outputFile);

            Log.v(GlobostoreAutoUpdate.TAG, "Store file to " + outputFile.getAbsolutePath());

            InputStream is = c.getInputStream();
            Log.v(GlobostoreAutoUpdate.TAG, "Start writing the file....");
            byte[] buffer = new byte[1024];
            int len1 = 0;

            while ((len1 = is.read(buffer)) != -1) {
                total += len1;
                long pgr = (total * 100 / fileLength);
                fos.write(buffer, 0, len1);
                this.handler.obtainMessage((int) pgr).sendToTarget();
            }
            Log.v(GlobostoreAutoUpdate.TAG, "File successfully downloaded and stored.");
            fos.flush();
            fos.close();
            is.close();
            callbackContext.success(1);

            installApp(context, outputFile);

            Log.v(GlobostoreAutoUpdate.TAG, "Started activity successfully!");
        } catch (Exception e) {
            callbackContext.error("Update error!");
            Log.e(GlobostoreAutoUpdate.TAG, "Error during processing", e);
        }
        return null;
    }

    private void checkFileUriExposure() {
        if (android.os.Build.VERSION.SDK_INT >= 24) {
            try {
                Method m = StrictMode.class.getMethod("disableDeathOnFileUriExposure");
                m.invoke(null);
            } catch (Exception e) {
                Log.e(GlobostoreAutoUpdate.TAG, "Error on disableing FileUriExposure for Android Build >= 24", e);
            }
        }
    }

    private void installApp(Context context, File outputFile) {
        Log.v(GlobostoreAutoUpdate.TAG, "Starting installation activity...");
        if(android.os.Build.VERSION.SDK_INT>25){
            Log.v(GlobostoreAutoUpdate.TAG, "Device SDK greater then 25...");
            Uri apkUri = FileProvider.getUriForFile(context, ".FileProvider", outputFile);
            Intent intent = new Intent(Intent.ACTION_INSTALL_PACKAGE);
            intent.setData(apkUri);
            intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        } else {
            Log.v(GlobostoreAutoUpdate.TAG, "Device SDK lesser then 25...");
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.setDataAndType(Uri.fromFile(outputFile), "application/vnd.android.package-archive");
            context.startActivity(intent);
        }
    }

    private String getPath(Context context) {
        if(android.os.Build.VERSION.SDK_INT>25) {
            return context.getCacheDir().getAbsolutePath();
        } else {
            return context.getExternalCacheDir().getAbsolutePath();
        }
    }
}