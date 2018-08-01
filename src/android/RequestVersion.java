package br.com.globostore.autoupdate;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;

public class RequestVersion extends AsyncTask<String , Void ,Version> {
    String server_response;

    private Map mapa;

    public void setMap(Map mapa) {
        this.mapa = mapa;
    }

    @Override
    protected Version doInBackground(String... strings) {

        URL url;
        HttpURLConnection urlConnection = null;
        Version version = new Version();


        try {
            url = new URL(strings[0]);

            urlConnection = (HttpURLConnection) url.openConnection();

            urlConnection.setDoOutput(true);
            urlConnection.setDoInput(true);
            urlConnection.setRequestMethod("POST");
            urlConnection.setRequestProperty("Content-Type", "application/json");
            urlConnection.setRequestProperty("Accept", "application/json");
            urlConnection.setRequestProperty ("appkey", mapa.get("appKey").toString());

            JSONObject body = new JSONObject();

            JSONObject request = new JSONObject();
            request.put("app_id", mapa.get("appId").toString());
            request.put("os", "android");

            body.put("request", request);
            body.put("method", mapa.get("method").toString());
            body.put("service", mapa.get("service").toString());

            OutputStream os = urlConnection.getOutputStream();
            os.write(body.toString().getBytes());
            os.flush();

            int responseCode = urlConnection.getResponseCode();
            String responseMessage = urlConnection.getResponseMessage();

            if(responseCode == HttpURLConnection.HTTP_OK){
                server_response = readStream(urlConnection.getInputStream());
                Log.v("CatalogClient", server_response);
                version = parseVersionData(server_response);
            } else {
                Log.v("CatalogClient", "Response code:"+ responseCode);
                Log.v("CatalogClient", "Response message:"+ responseMessage);
                throw new Exception();
            }

        } catch (JSONException je) { 
            je.printStackTrace();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            if(urlConnection != null)
                urlConnection.disconnect();
        }

        return version;
    }

    @Override
    protected void onPostExecute(Version version) {
        super.onPostExecute(version);
    }

    // Converting InputStream to String
    private String readStream(InputStream in) {
        BufferedReader reader = null;
        StringBuffer response = new StringBuffer();
        try {
            reader = new BufferedReader(new InputStreamReader(in));
            String line = "";
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return response.toString();
    }

    private Version parseVersionData(String jString){

        Version version = new Version();

        try {
            JSONObject jObj = new JSONObject(jString);
            JSONObject versionResp = jObj.getJSONObject("version");
            version.setVersion(versionResp.getString("version"));
            version.setDownloadUrl(versionResp.getString("download"));
        } catch (JSONException e) {
            Log.e("parseVersionData", "unexpected JSON exception", e);
        }

        return version;
    }

}
