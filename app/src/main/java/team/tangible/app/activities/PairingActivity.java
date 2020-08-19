package team.tangible.app.activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.polidea.rxandroidble2.RxBleClient;
import com.polidea.rxandroidble2.RxBleConnection;
import com.polidea.rxandroidble2.RxBleDevice;
import com.polidea.rxandroidble2.exceptions.BleDisconnectedException;
import com.polidea.rxandroidble2.scan.ScanResult;
import com.polidea.rxandroidble2.scan.ScanSettings;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import butterknife.BindView;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import team.tangible.app.Configuration;
import team.tangible.app.R;
import team.tangible.app.utils.ArrayUtils;
import team.tangible.app.utils.RxBleUtils;
import team.tangible.app.utils.TangibleUtils;

import static team.tangible.app.Configuration.SharedPreferences.Keys.PAIRED_BLE_DEVICE_MAC_ADDRESS;
import static team.tangible.app.Configuration.SharedPreferences.NAME;

public class PairingActivity extends BaseActivity {

    private static final String TAG = PairingActivity.class.getName();
    private static final int BLUETOOTH_PERMISSIONS_REQUEST_CODE = 1;

    @BindView(R.id.activity_pairing_status)
    TextView mStatusTextView;

    @BindView(R.id.activity_pairing_scanned_peripherals)
    ListView mScannedPeripheralsListView;

    @BindView(R.id.activity_pairing_continue_without_pairing)
    Button mContinueWithoutPairingButton;

    private ScannedPeripheralsAdapter mScannedPeripheralsAdapter = new ScannedPeripheralsAdapter();
    private Disposable mScanSubscription;
    private RxBleClient mRxBleClient;

    private MutableLiveData<List<RxBleDevice>> mRxBleDevicesLiveData = new MutableLiveData<List<RxBleDevice>>() {{
        setValue(new ArrayList<>());
    }};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pairing);

        bind();

        mContinueWithoutPairingButton.setOnClickListener(view -> {
            Intent moveToLoginIntent = new Intent(PairingActivity.this, LoginActivity.class);
            startActivity(moveToLoginIntent);
        });

        mScannedPeripheralsListView.setAdapter(mScannedPeripheralsAdapter);

        // We want to block here so we know if we have a result or not
        TangibleUtils.getTangibleBleConnection(PairingActivity.this).subscribe(rxBleConnection -> {
            Log.i(TAG, "BLE device + connection available for already paired device");
            Intent moveToLoginIntent = new Intent(PairingActivity.this, LoginActivity.class);
            PairingActivity.this.startActivity(moveToLoginIntent);
        }, throwable -> {
            // throw new RuntimeException(throwable);
        });

        mRxBleClient = RxBleClient.create(PairingActivity.this);

        if (!mRxBleClient.isScanRuntimePermissionGranted()) {
            String[] rxBleClientPermissions = mRxBleClient.getRecommendedScanRuntimePermissions();
            // Merge the permissions from the client and from what we'd like
            String[] permissionsToRequest = ArrayUtils.joinDistinct(rxBleClientPermissions, Configuration.RequiredPermissions.BLUETOOTH_LOW_ENERGY);
            this.requestPermissions(permissionsToRequest, BLUETOOTH_PERMISSIONS_REQUEST_CODE);
        } else {
            onAllPermissionsGranted();
        }

        mRxBleDevicesLiveData.observe(PairingActivity.this, rxBleDevices -> {
            if (rxBleDevices.isEmpty()) {
                mStatusTextView.setText(R.string.no_bluetooth_devices_found_searching);
            } else {
                mStatusTextView.setText(getResources().getString(R.string.bluetooth_devices_found, rxBleDevices.size()));
            }
        });

        mRxBleDevicesLiveData.observe(PairingActivity.this, mScannedPeripheralsAdapter);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == BLUETOOTH_PERMISSIONS_REQUEST_CODE) {
            boolean werePermissionsGranted = Arrays.stream(grantResults).allMatch(result -> result == PackageManager.PERMISSION_GRANTED);

            if (werePermissionsGranted) {
                onAllPermissionsGranted();
            } else {
                Toast.makeText(PairingActivity.this, "Permissions denied.", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void onAllPermissionsGranted() {
        Log.i(TAG, "All required permissions were granted, starting BLE scan");
        startBleDeviceScan();
    }

    private void startBleDeviceScan() {
        mScanSubscription = mRxBleClient.scanBleDevices(new ScanSettings.Builder().build())
                .subscribe(
                        (ScanResult scanResult) -> {
                            // Process scan result here.
                            RxBleDevice bleDevice = scanResult.getBleDevice();
                            if (bleDevice != null && bleDevice.getName() != null) {
                                List<RxBleDevice> devicesList = Objects.requireNonNull(mRxBleDevicesLiveData.getValue());

                                if (!devicesList.contains(bleDevice)) {
                                    devicesList.add(bleDevice);
                                    mRxBleDevicesLiveData.setValue(devicesList);
                                    Log.i(TAG, "Added BLE Device to live data: " + bleDevice.getName());
                                } else {
                                    Log.d(TAG, "BLE Device already in live data: " + bleDevice.getName());
                                }

                            }
                        },
                        (Throwable throwable) -> {
                            // Handle an error here.
                            Log.e(TAG, throwable.toString());
                        }
                );
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (mScanSubscription != null) {
            mScanSubscription.dispose();
            mScanSubscription = null;
        }
    }

    /**
     * A ListView adapter that syncs the discovered devices with the UI
     */
    private class ScannedPeripheralsAdapter extends BaseAdapter implements Observer<List<RxBleDevice>> {

        List<RxBleDevice> discoveredPeripherals = new ArrayList<>();

        @Override
        public int getCount() {
            return discoveredPeripherals.size();
        }

        @Override
        public Object getItem(int position) {
            return discoveredPeripherals.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = getLayoutInflater().inflate(R.layout.activity_pairing_scanned_peripherals_list_item, parent, false);
            }

            RxBleDevice bleDevice = (RxBleDevice) getItem(position);

            // Bind text and listeners to list item Views
            ((TextView) convertView.findViewById(R.id.text_view_device_name)).setText(bleDevice.getName());
            ((TextView) convertView.findViewById(R.id.text_view_device_address)).setText(bleDevice.getMacAddress());
            ((TextView) convertView.findViewById(R.id.text_view_device_status)).setText(RxBleUtils.getConnectionStateString(bleDevice.getConnectionState()));
            ((Button) convertView.findViewById(R.id.button_connect)).setOnClickListener((View.OnClickListener) (View v) -> {
                new AlertDialog.Builder(PairingActivity.this)
                        .setTitle("Confirm Tangible pairing")
                        .setMessage("Is this device your Tangible?\n" + bleDevice.getName())
                        .setCancelable(false)
                        .setNegativeButton("Cancel", null)
                        .setPositiveButton("Confirm pairing", (DialogInterface dialog, int which) -> {
                            dialog.dismiss();
                            SharedPreferences sharedPreferences = PairingActivity.this.getSharedPreferences(NAME, Context.MODE_PRIVATE);
                            SharedPreferences.Editor sharedPreferencesEditor = sharedPreferences.edit();
                            sharedPreferencesEditor.putString(PAIRED_BLE_DEVICE_MAC_ADDRESS, bleDevice.getMacAddress());
                            if (!sharedPreferencesEditor.commit()) {
                                dialog.cancel();
                                runOnUiThread(() -> {
                                    Log.e(TAG, getResources().getString(R.string.failed_to_save_bluetooth_mac_address));
                                    mStatusTextView.setText(R.string.failed_to_save_bluetooth_mac_address);
                                });
                                return;
                            };

                            Intent intent = new Intent(PairingActivity.this, LoginActivity.class);
                            PairingActivity.this.startActivity(intent);

                            Toast.makeText(PairingActivity.this, "Paired with " + bleDevice.getName(), Toast.LENGTH_LONG).show();
                        })
                        .create()
                        .show();
            });

            return convertView;
        }

        @Override
        public void onChanged(List<RxBleDevice> rxBleDevices) {
            discoveredPeripherals = rxBleDevices;
            notifyDataSetChanged();
        }
    }
}