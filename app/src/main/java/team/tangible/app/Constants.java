package team.tangible.app;

import android.Manifest;

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
        public static final String NAME = "team.tangible.app";
        public static class Keys {
            public static final String PAIRED_BLE_DEVICE_MAC_ADDRESS = "PAIRED_BLE_DEVICE_MAC_ADDRESS";
        }
    }
}
