package team.tangible.app;

import android.Manifest;

public class Configuration {
    public static final String VERSION = "0.0.1";
    public static final boolean DEBUG = true;

    public static class RequiredPermissions {
        public static final String[] BLUETOOTH_LOW_ENERGY = new String[]{
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.ACCESS_FINE_LOCATION
        };
    }
}
