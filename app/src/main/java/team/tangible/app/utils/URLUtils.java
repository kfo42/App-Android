package team.tangible.app.utils;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

public class URLUtils {
    public static URL parse(String uri) {
        try {
            return new URL(uri);
        } catch (MalformedURLException e) {
            e.printStackTrace();
            return null;
        }
    }
}


