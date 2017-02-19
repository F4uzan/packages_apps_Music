package com.dominionos.music.task;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.widget.Toast;

import com.afollestad.async.Action;
import com.dominionos.music.R;
import com.dominionos.music.utils.ArtistImgHandler;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Calendar;

public class FetchArtistImg {

    private final int random;
    private final ArtistImgHandler handler;
    private String url;
    private final Context context;
    private final String name;

    public FetchArtistImg(Context context, String name, int random, ArtistImgHandler handler) {
        this.context = context;
        this.name = name;
        this.random = random;
        this.handler = handler;
        if (name != null) {
            StringBuilder builder = new StringBuilder();
            builder.append(context.getResources().getString(R.string.artist_fetch_url));
            try {
                builder.append("&artist=").append(URLEncoder.encode(name, "UTF-8"));
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            builder.append("&api_key=").append(context.getResources().getString(R.string.api));
            builder.append("&format=json");
            this.url = builder.toString();
            runTask();
        }
    }

    private void runTask() {
        new Action<String>() {
            @NonNull
            @Override
            public String id() {
                return name;
            }

            @Override
            protected String run() throws InterruptedException {
                backgroundTask();
                return null;
            }

            @Override
            protected void done(@Nullable String result) {
            }
        }.execute();
    }

    private void backgroundTask() {
        try {
            URL url = new URL(this.url);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            String jsonResult = inputStreamToString(new BufferedInputStream(connection.getInputStream()))
                    .toString();
            try {
                JSONObject jsonResponse = new JSONObject(jsonResult);
                JSONArray imageArray = jsonResponse.getJSONObject("artist").getJSONArray("image");
                for (int i = 0; i < imageArray.length(); i++) {
                    JSONObject image = imageArray.getJSONObject(i);
                    if (image.optString("size").matches("large") &&
                            !image.optString("#text").matches("")) {
                        Bitmap downloadedImg = downloadBitmap(image.optString("#text"));
                        String newUrl = saveImageToStorage(downloadedImg);
                        handler.updateArtistArtWorkInDB(name, newUrl);
                        handler.onDownloadComplete(newUrl);
                    }
                }
            } catch (JSONException ignored) {}
        } catch (IOException ignored) {}
    }

    private String saveImageToStorage(Bitmap bitmap) {
        StringBuilder fileName = new StringBuilder();
        fileName.append("cache-img-");
        Calendar c = Calendar.getInstance();
        fileName.append(c.get(Calendar.DATE)).append("-");
        fileName.append(c.get(Calendar.MONTH)).append("-");
        fileName.append(c.get(Calendar.YEAR)).append("-");
        fileName.append(random).append("-");
        fileName.append((random / 3) * 5);
        fileName.append(".png");
        File sdCardDirectory = Environment.getExternalStorageDirectory();
        String filePath = sdCardDirectory + "/" + context.getResources()
                .getString(R.string.app_name) + "/artist/";
        (new File(filePath)).mkdirs();
        File noMedia = new File(filePath, ".nomedia");
        if (!noMedia.exists()) {
            try {
                noMedia.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        File image = new File(filePath, fileName.toString());
        if (image.exists()) {
            boolean deleted = image.delete();
            if(!deleted) {
                Toast.makeText(context, "Failed to delete unused resources", Toast.LENGTH_SHORT).show();
            }
        }
        try {
            FileOutputStream out = new FileOutputStream(image);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
            out.flush();
            out.close();
            return image.getPath();
        } catch (IOException | NullPointerException e) {
            e.printStackTrace();
        }
        return null;
    }

    private Bitmap downloadBitmap(String urlString) {

        HttpURLConnection connection = null;
        try {
            URL url = new URL(urlString);
            connection = (HttpURLConnection) url.openConnection();
            InputStream in = new BufferedInputStream(connection.getInputStream());
            return BitmapFactory.decodeStream(in);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if(connection != null) {
                connection.disconnect();
            }
        }

        return null;
    }

    private StringBuilder inputStreamToString(InputStream is) {
        String rLine;
        StringBuilder answer = new StringBuilder();
        BufferedReader rd = new BufferedReader(new InputStreamReader(is));

        try {
            while ((rLine = rd.readLine()) != null) {
                answer.append(rLine);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return answer;
    }
}
