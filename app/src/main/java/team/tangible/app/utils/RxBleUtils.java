package team.tangible.app.utils;

import com.polidea.rxandroidble2.RxBleConnection;

public class RxBleUtils {
    public static String getConnectionStateString(RxBleConnection.RxBleConnectionState connectionState) {
        if (connectionState == null) {
            throw new NullPointerException("Argument connectionState is null");
        }
        switch (connectionState) {
            case CONNECTED:
                return "connected";
            case CONNECTING:
                return "connecting";
            case DISCONNECTED:
                return "disconnected";
            case DISCONNECTING:
                return "disconnecting";
            default:
                throw new RuntimeException(connectionState.toString());
        }
    }
}
