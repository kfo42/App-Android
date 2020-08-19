package team.tangible.app.utils;

import android.content.Context;
import android.content.SharedPreferences;

import com.polidea.rxandroidble2.RxBleClient;
import com.polidea.rxandroidble2.RxBleConnection;

import io.reactivex.Observable;
import io.reactivex.functions.Consumer;
import team.tangible.app.Configuration;

public class TangibleUtils {
    public static Observable<RxBleConnection> getTangibleBleConnection(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(Configuration.SharedPreferences.NAME, Context.MODE_PRIVATE);
        String savedMacAddress = sharedPreferences.getString(Configuration.SharedPreferences.Keys.PAIRED_BLE_DEVICE_MAC_ADDRESS, null);

        if (savedMacAddress == null) {
            return Observable.error(new NoPairedTangibleException());
        }

        RxBleClient rxBleClient = RxBleClient.create(context);

        return rxBleClient.getBleDevice(savedMacAddress).establishConnection(false);
    }

    public static class NoPairedTangibleException extends RuntimeException {

    }
}
