package team.tangible.app;

import android.Manifest;
import android.content.Intent;

import com.firebase.ui.auth.AuthUI;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class Constants {
    public static class RequiredPermissions {
        public static final String[] BLUETOOTH_LOW_ENERGY = new String[]{
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.ACCESS_FINE_LOCATION
        };
    }

    public static class BluetoothLowEnergy {

        /**
         * https://developer.nordicsemi.com/nRF_Connect_SDK/doc/latest/nrf/include/bluetooth/services/nus.html
         */
        public static class NordicUARTService {

            public static final UUID SERVICE_UUID = UUID.fromString("6E400001-B5A3-F393-E0A9-E50E24DCCA9E");

            public static class Characteristics {
                /**
                 * Write to the RX characteristic since that's where the board is listening
                 */
                public static final UUID RX = UUID.fromString("6E400002-B5A3-F393-E0A9-E50E24DCCA9E");

                /**
                 * Read from the TX characteristic since that's where the board is sending
                 */
                public static final UUID TX = UUID.fromString("6E400003-B5A3-F393-E0A9-E50E24DCCA9E");
            }
        }
    }

    public static class SharedPreferences {
        public static final String TEAM_TANGIBLE_APP = "team.tangible.app";
        public static class Keys {
            public static final String PAIRED_BLE_DEVICE_MAC_ADDRESS = "PAIRED_BLE_DEVICE_MAC_ADDRESS";
        }
    }

    public static class Firebase {
        public static class Authentication {
            public static final List<AuthUI.IdpConfig> PROVIDERS = Collections.singletonList(
                    new AuthUI.IdpConfig.EmailBuilder().build());
            public static final Intent FIREBASE_AUTH_UI_INTENT = AuthUI.getInstance()
                    .createSignInIntentBuilder()
                    .setAvailableProviders(PROVIDERS)
                    .build();
        }
    }

    public static class Toast {
        // This duration maps to {@see Toast.LENGTH_LONG}
        public static final int TOAST_LENGTH_LONG_MS = 3500;
    }

    public class Threading {
        public static final String MAIN_THREAD = "MAIN_THREAD";
    }
}
