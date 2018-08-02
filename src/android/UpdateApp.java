package br.com.globostore.autoupdate;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.Exception;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import org.apache.cordova.CallbackContext;
import java.net.URL;

import android.os.StrictMode;
import android.util.Log;

public class UpdateApp extends AsyncTask<String, Void, Void> {
    private Context context;
    private CallbackContext callbackContext;

    public void setContext(Context contextf) {
        context = contextf;
    }

    public void setCallback(CallbackContext callbackContextf) {
        this.callbackContext = callbackContextf;
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
//            c.setDoOutput(true);
            c.connect();

            // this will be useful to display download percentage
            // might be -1: server did not report the length
            int fileLength = c.getContentLength();
            long total = 0;

            String PATH = context.getExternalCacheDir().getAbsolutePath();
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
                Log.v(GlobostoreAutoUpdate.TAG, "Writing...."+(total * 100 / fileLength) + "%");
                fos.write(buffer, 0, len1);
            }
            Log.v(GlobostoreAutoUpdate.TAG, "File successfully downloaded and stored.");
            fos.flush();
            fos.close();
            is.close();
            callbackContext.success(1);

            Log.v(GlobostoreAutoUpdate.TAG, "Starting activity...");
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.setDataAndType(Uri.fromFile(outputFile),
                    "application/vnd.android.package-archive");
            context.startActivity(intent);
            
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
}