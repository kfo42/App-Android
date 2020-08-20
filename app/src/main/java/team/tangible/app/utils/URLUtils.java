package team.tangible.app.utils;
import android.util.Log;

import androidx.annotation.NonNull;

import java.net.MalformedURLException;
import java.net.URL;

public class URLUtils {
    private static final String TAG = URLUtils.class.getName();

    public static URL parse(@NonNull String url) {
        try {
            return new URL(url);
        } catch (MalformedURLException e) {
            Log.e(TAG, "Failed to parse URL from string " + url, e);
            e.printStackTrace();
            return null;
        }
    }
}
