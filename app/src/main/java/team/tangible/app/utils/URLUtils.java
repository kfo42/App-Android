package team.tangible.app.utils;
import android.util.Log;

import androidx.annotation.NonNull;

import java.net.MalformedURLException;
import java.net.URL;

import timber.log.Timber;

public class URLUtils {
    public static URL parse(@NonNull String url) {
        try {
            return new URL(url);
        } catch (MalformedURLException e) {
            Timber.e(e, "Failed to parse URL from string %s", url);
            e.printStackTrace();
            return null;
        }
    }
}
