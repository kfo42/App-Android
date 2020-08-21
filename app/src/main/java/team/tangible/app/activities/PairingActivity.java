package team.tangible.app.activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.polidea.rxandroidble2.RxBleClient;
import com.polidea.rxandroidble2.RxBleDevice;
import com.polidea.rxandroidble2.scan.ScanResult;
import com.polidea.rxandroidble2.scan.ScanSettings;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.disposables.Disposable;
import team.tangible.app.Constants;
import team.tangible.app.R;
import team.tangible.app.utils.ActivityUtils;
import team.tangible.app.utils.ArrayUtils;
import team.tangible.app.utils.RxBleUtils;
import team.tangible.app.utils.TangibleUtils;
import timber.log.Timber;

import static co.apptailor.googlesignin.RNGoogleSigninModule.RC_SIGN_IN;
import static team.tangible.app.Constants.Firebase.Authentication.FIREBASE_AUTH_UI_INTENT;
import static team.tangible.app.Constants.SharedPreferences.NAME;
import static team.tangible.app.Constants.Toast.TOAST_LENGTH_LONG_MS;
import static team.tangible.app.Constants.SharedPreferences.Keys;

public class PairingActivity extends AppCompatActivity {

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
    private Handler mMainThreadHandler;
    private Disposable mConnectionCheckingSubscription;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pairing);

        ButterKnife.bind(this);

        mMainThreadHandler = new Handler(this.getMainLooper());

        mContinueWithoutPairingButton.setOnClickListener(view -> {
            Intent moveToLoginIntent = new Intent(PairingActivity.this, LoginActivity.class);
            startActivity(moveToLoginIntent);
        });

        mScannedPeripheralsListView.setAdapter(mScannedPeripheralsAdapter);

        // We want to block here so we know if we have a result or not
        mConnectionCheckingSubscription = TangibleUtils.getTangibleBleConnection(PairingActivity.this).subscribe(rxBleConnection -> {
            Timber.i("BLE device + connection available for already paired device");
            Intent moveToLoginIntent = new Intent(PairingActivity.this, HomescreenActivity.class);
            PairingActivity.this.startActivity(moveToLoginIntent);
            PairingActivity.this.finish();
        }, throwable -> {
            // throw new RuntimeException(throwable);
        });

        mRxBleClient = RxBleClient.create(PairingActivity.this);

        if (!mRxBleClient.isScanRuntimePermissionGranted()) {
            String[] rxBleClientPermissions = mRxBleClient.getRecommendedScanRuntimePermissions();
            // Merge the permissions from the client and from what we'd like
            String[] permissionsToRequest = ArrayUtils.joinDistinct(rxBleClientPermissions, Constants.RequiredPermissions.BLUETOOTH_LOW_ENERGY);
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
        Timber.i("All required permissions were granted, starting BLE scan");
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
                                    Timber.i("Added BLE Device to live data: %s", bleDevice.getName());
                                } else {
                                    Timber.d("BLE Device already in live data: %s", bleDevice.getName());
                                }

                            }
                        },
                        (Throwable throwable) -> {
                            // Handle an error here.
                            Timber.e(throwable);
                        }
                );
    }

    @Override
    protected void onStop() {
        super.onStop();

        if (mScanSubscription != null) {
            mScanSubscription.dispose();
            mScanSubscription = null;
        }

        if (mConnectionCheckingSubscription != null) {
            mConnectionCheckingSubscription.dispose();
            mConnectionCheckingSubscription = null;
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
            ((Button) convertView.findViewById(R.id.button_connect)).setOnClickListener((View v) -> {
                new AlertDialog.Builder(PairingActivity.this)
                        .setTitle("Confirm Tangible pairing")
                        .setMessage("Is this device your Tangible?\n" + bleDevice.getName())
                        .setCancelable(false)
                        .setNegativeButton("Cancel", null)
                        .setPositiveButton("Confirm pairing",
                                new PositiveButtonConfirmPairingOnClickListener(bleDevice))
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

    private class PositiveButtonConfirmPairingOnClickListener implements DialogInterface.OnClickListener {
        private RxBleDevice mRxBleDevice;

        public PositiveButtonConfirmPairingOnClickListener(RxBleDevice rxBleDevice) {
            this.mRxBleDevice = rxBleDevice;
        }

        @Override
        public void onClick(DialogInterface dialog, int which) {
            dialog.dismiss();

            Toast.makeText(PairingActivity.this, "Paired with " + mRxBleDevice.getName(), Toast.LENGTH_LONG).show();

            SharedPreferences sharedPreferences = PairingActivity.this.getSharedPreferences(NAME, Context.MODE_PRIVATE);
            SharedPreferences.Editor sharedPreferencesEditor = sharedPreferences.edit();
            sharedPreferencesEditor.putString(Keys.PAIRED_BLE_DEVICE_MAC_ADDRESS, mRxBleDevice.getMacAddress());

            if (!sharedPreferencesEditor.commit()) {
                // If the saving to SharedPreferences failed...

                Timber.e("Failed to save MAC address %s to %s", mRxBleDevice.getMacAddress(), SharedPreferences.class.getSimpleName());

                dialog.cancel();

                runOnUiThread(() -> {

                    Timber.e(getResources().getString(R.string.failed_to_save_bluetooth_mac_address));
                    mStatusTextView.setText(R.string.failed_to_save_bluetooth_mac_address);

                    new AlertDialog.Builder(PairingActivity.this)
                            .setTitle("Failed to pair to Bluetooth device")
                            .setMessage(mRxBleDevice.getName())
                            .setCancelable(false)
                            .setPositiveButton("Try again...", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                }
                            })
                            .create()
                            .show();

                });

                return;
            }

            boolean isUserLoggedIn = FirebaseAuth.getInstance().getCurrentUser() != null;

            if (isUserLoggedIn) {
                // Then move to the Homescreen! We're done with all the setup
                Intent intent = new Intent(PairingActivity.this, HomescreenActivity.class);
                PairingActivity.this.startActivity(intent);
                PairingActivity.this.finish();

            } else {
                // User isn't logged in so

                Timber.w("User is not logged in. Moving to Firebase Auth UI");

                // If they are not signed in (the user is null), then start the sign in UI
                mMainThreadHandler.postDelayed(() -> {

                    Toast.makeText(PairingActivity.this, "Let's get you signed in...", Toast.LENGTH_LONG).show();

                    // Create and launch sign-in intent
                    startActivityForResult(FIREBASE_AUTH_UI_INTENT, RC_SIGN_IN);

                }, TOAST_LENGTH_LONG_MS);

            }

        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);

        if (hasFocus) {
            ActivityUtils.hideSystemUI(this);
        }
    }
}