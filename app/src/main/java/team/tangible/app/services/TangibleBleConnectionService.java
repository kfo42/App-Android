package team.tangible.app.services;

import android.content.SharedPreferences;

import com.polidea.rxandroidble2.RxBleClient;
import com.polidea.rxandroidble2.RxBleConnection;
import com.polidea.rxandroidble2.scan.ScanResult;
import com.polidea.rxandroidble2.scan.ScanSettings;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.Single;
import team.tangible.app.Constants;
import team.tangible.app.results.TangibleAvailabilityResult;
import timber.log.Timber;

import static team.tangible.app.results.TangibleAvailabilityResult.*;

public class TangibleBleConnectionService {
    private static final long AVAILABILITY_TIMEOUT_MS = 5000;
    private final RxBleClient mRxBleClient;
    private final SharedPreferences mSharedPreferences;

    public TangibleBleConnectionService(RxBleClient rxBleClient, SharedPreferences sharedPreferences) {
        mRxBleClient = rxBleClient;
        mSharedPreferences = sharedPreferences;
    }

    public boolean isMacAddressSaved() {
        return getSavedMacAddress() != null;
    }

    public String getSavedMacAddress() {
        return mSharedPreferences.getString(Constants.SharedPreferences.Keys.PAIRED_BLE_DEVICE_MAC_ADDRESS, null);
    }

    public boolean setSavedMacAddress(String macAddress) {
        return mSharedPreferences.edit()
                .putString(Constants.SharedPreferences.Keys.PAIRED_BLE_DEVICE_MAC_ADDRESS, macAddress)
                .commit();
    }

    public Single<TangibleAvailabilityResult> isTangibleAvailable() {
        if (!areRuntimePermissionsGranted()) {
            return Single.error(new RuntimePermissionsNotGranted());
        }

        if (!isMacAddressSaved()) {
            return Single.just(NOT_PAIRED);
        }

        String savedMacAddress = getSavedMacAddress();

        return mRxBleClient.scanBleDevices(new ScanSettings.Builder().build()).any(scanResult -> {
            // We want to check if the saved MAC address is in the scanned vicinity
            return Objects.equals(scanResult.getBleDevice().getMacAddress(), savedMacAddress);
        }).map((Boolean wasTangibleFound) -> {
            return wasTangibleFound ? AVAILABLE : NOT_FOUND;
        }).timeout(AVAILABILITY_TIMEOUT_MS, TimeUnit.MILLISECONDS).onErrorReturn(throwable -> {
            Timber.e(throwable);
            return NOT_FOUND;
        });
    }

    public boolean areRuntimePermissionsGranted() {
        return mRxBleClient.isScanRuntimePermissionGranted();
    }

    public String[] getRuntimePermissions() {
        return mRxBleClient.getRecommendedScanRuntimePermissions();
    }

    public Observable<ScanResult> scanBleDevices() {
        return mRxBleClient.scanBleDevices(new ScanSettings.Builder().build());
    }

    public Observable<RxBleConnection> getConnection() {
        if (!isMacAddressSaved()) {
            return Observable.error(new NoPairedTangibleException());
        }

        String savedMacAddress = getSavedMacAddress();
        return mRxBleClient.getBleDevice(savedMacAddress).establishConnection(false);
    }

    public static class NoPairedTangibleException extends Error {}

    public static class RuntimePermissionsNotGranted extends Error {}
}
