package team.tangible.app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.MutableLiveData;

import butterknife.ButterKnife;
import io.reactivex.disposables.Disposable;
import team.tangible.app.R;
import team.tangible.app.utils.ActivityUtils;
import team.tangible.app.utils.TangibleUtils;

public class SplashActivity extends AppCompatActivity {

    private static final String TAG = SplashActivity.class.getName();

    private static final int TOAST_LENGTH_LONG_MS = 3500;

    private MutableLiveData<Boolean> mIsUserPairedWithTangibleLiveData = new MutableLiveData<Boolean>() {{ setValue(false);}};
    // TODO: Implement user auth and incorporate into this logic. Chain observables?

    private Disposable mSubscriptionDisposable;

    private Handler mMainThreadHandler;

    /**
     * In this method, we want to check a few things to direct the user to the right activity:
     * 1) Whether or not they are logged into the app
     * 2) Whether or not they have paired a Tangible and if it can support a connection
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        ButterKnife.bind(this);

        mMainThreadHandler = new Handler(this.getMainLooper());

        mIsUserPairedWithTangibleLiveData.observe(this, (Boolean value) -> {
            String toastText = value ? "Your Tangible is ready to go!" : "Let's pair your Tangible...";
            Toast.makeText(SplashActivity.this, toastText, Toast.LENGTH_LONG).show();

            // If the Tangible is paired, go to Homescreen. If not, go to Pairing
            Class<?> nextActivityClass = value ? HomescreenActivity.class : PairingActivity.class;

            mMainThreadHandler.postDelayed(() -> {
                /* Create an Intent that will start the next screen after the toast */
                Intent mainIntent = new Intent(SplashActivity.this, nextActivityClass);
                SplashActivity.this.startActivity(mainIntent);
            }, TOAST_LENGTH_LONG_MS);
        });

        // We want to block here so we know if we have a result or not
        mSubscriptionDisposable = TangibleUtils.getTangibleBleConnection(SplashActivity.this).subscribe(rxBleConnection -> {
            Log.i(TAG, "User is paired with Tangible");
            // Live data must be changed on the main thread
            mMainThreadHandler.post(() -> mIsUserPairedWithTangibleLiveData.setValue(true));
        }, throwable -> {
            Log.e(TAG, "User is NOT paired with Tangible", throwable);
            mMainThreadHandler.post(() -> mIsUserPairedWithTangibleLiveData.setValue(false));
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (mSubscriptionDisposable != null) {
            mSubscriptionDisposable.dispose();
            mSubscriptionDisposable = null;
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