package team.tangible.app.activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;

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

import com.polidea.rxandroidble2.RxBleDevice;
import com.polidea.rxandroidble2.scan.ScanResult;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import javax.inject.Inject;
import javax.inject.Named;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import team.tangible.app.BuildConfig;
import team.tangible.app.Constants;
import team.tangible.app.R;
import team.tangible.app.TangibleApplication;
import team.tangible.app.results.LoginResult;
import team.tangible.app.results.TangibleAvailabilityResult;
import team.tangible.app.services.AuthenticationService;
import team.tangible.app.services.TangibleBleConnectionService;
import team.tangible.app.utils.ActivityUtils;
import team.tangible.app.utils.ArrayUtils;
import timber.log.Timber;

import static co.apptailor.googlesignin.RNGoogleSigninModule.RC_SIGN_IN;
import static team.tangible.app.Constants.Firebase.Authentication.FIREBASE_AUTH_UI_INTENT;
import static team.tangible.app.Constants.Toast.TOAST_LENGTH_LONG_MS;

public class PairingActivity extends AppCompatActivity {

    private static final int BLUETOOTH_PERMISSIONS_REQUEST_CODE = 42;

    @BindView(R.id.activity_pairing_status)
    TextView mStatusTextView;

    @BindView(R.id.activity_pairing_scanned_peripherals)
    ListView mScannedPeripheralsListView;

    @BindView(R.id.activity_pairing_continue_without_pairing)
    Button mContinueWithoutPairingButton;

    @Inject
    TangibleBleConnectionService mTangibleBleConnectionService;

    private ScannedPeripheralsAdapter mScannedPeripheralsAdapter = new ScannedPeripheralsAdapter();

    private Disposable mScanSubscription;

    private MutableLiveData<List<RxBleDevice>> mRxBleDevicesLiveData = new MutableLiveData<>(new ArrayList<>());

    @Inject
    @Named(Constants.Threading.MAIN_THREAD)
    Handler mMainThreadHandler;

    @Inject
    AuthenticationService mAuthenticationService;

    private CompositeDisposable mDisposables;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pairing);

        ButterKnife.bind(this);

        ((TangibleApplication) getApplication()).getApplicationComponent().inject(this);

        // Only present the "Continue without Pairing" button in debug mode
        if (BuildConfig.DEBUG) {
            mContinueWithoutPairingButton.setVisibility(View.VISIBLE);
            mContinueWithoutPairingButton.setOnClickListener(view -> {
                startActivity(new Intent(PairingActivity.this, HomescreenActivity.class));
                finish();
            });
        }

        // Make the list view respond to changes through this adapter
        mScannedPeripheralsListView.setAdapter(mScannedPeripheralsAdapter);

        // Listen for changes to the available BLE devices and post to the adapter
        mRxBleDevicesLiveData.observe(this, mScannedPeripheralsAdapter);
    }

    @Override
    protected void onStart() {
        super.onStart();

        mDisposables.add(mTangibleBleConnectionService.isTangibleAvailable().subscribe(tangibleAvailabilityResult -> {
            if (tangibleAvailabilityResult == TangibleAvailabilityResult.AVAILABLE) {
                Timber.i("BLE device + connection available for already paired device");
                startActivity(new Intent(PairingActivity.this, HomescreenActivity.class));
                finish();
            }
        }));

        if (!mTangibleBleConnectionService.areRuntimePermissionsGranted()) {
            String[] rxBleClientPermissions = mTangibleBleConnectionService.getRuntimePermissions();
            // Merge the permissions from the client and from what we'd like
            String[] permissionsToRequest = ArrayUtils.joinDistinct(rxBleClientPermissions, Constants.RequiredPermissions.BLUETOOTH_LOW_ENERGY);
            this.requestPermissions(permissionsToRequest, BLUETOOTH_PERMISSIONS_REQUEST_CODE);
        } else {
            onAllPermissionsGranted();
        }

        mRxBleDevicesLiveData.observe(this, rxBleDevices -> {
            if (rxBleDevices.isEmpty()) {
                mStatusTextView.setText(R.string.no_bluetooth_devices_found_searching);
            } else {
                mStatusTextView.setText(getResources().getString(R.string.bluetooth_devices_found, rxBleDevices.size()));
            }
        });
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
        mScanSubscription = mTangibleBleConnectionService.scanBleDevices().subscribe(
            (ScanResult scanResult) -> {
                // Process scan result here
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

        if (mDisposables != null) {
            mDisposables.dispose();
            mDisposables = null;
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

            if (!mTangibleBleConnectionService.setSavedMacAddress(mRxBleDevice.getMacAddress())) {
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
                            .setPositiveButton("Try again...", (dialog1, which1) -> dialog1.dismiss())
                            .create()
                            .show();

                });

                return;
            }

            mDisposables.add(mAuthenticationService.isUserLoggedIn().subscribe(result -> {
                if (result == LoginResult.SUCCESS) {
                    // Then move to the Homescreen! We're done with all the setup
                    startActivity(new Intent(PairingActivity.this, HomescreenActivity.class));
                    finish();
                } else {
                    // User isn't logged in so log them in
                    Timber.w("User is not logged in. Moving to Firebase Auth UI");

                    // If they are not signed in (the user is null), then start the sign in UI
                    mMainThreadHandler.postDelayed(() -> {

                        Toast.makeText(PairingActivity.this, "Let's get you signed in...", Toast.LENGTH_LONG).show();

                        // Create and launch sign-in intent
                        startActivityForResult(FIREBASE_AUTH_UI_INTENT, RC_SIGN_IN);

                    }, TOAST_LENGTH_LONG_MS);
                }
            }));
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