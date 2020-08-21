package team.tangible.app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;

import com.firebase.ui.auth.IdpResponse;
import com.google.firebase.auth.FirebaseAuth;

import butterknife.ButterKnife;
import io.reactivex.disposables.Disposable;
import team.tangible.app.R;
import team.tangible.app.utils.ActivityUtils;
import team.tangible.app.utils.FirebaseAuthUtils;
import team.tangible.app.utils.TangibleUtils;
import timber.log.Timber;

import static co.apptailor.googlesignin.RNGoogleSigninModule.RC_SIGN_IN;
import static team.tangible.app.Constants.Firebase.Authentication.FIREBASE_AUTH_UI_INTENT;
import static team.tangible.app.Constants.Toast.TOAST_LENGTH_LONG_MS;

public class SplashActivity extends AppCompatActivity {

    private static final String TAG = SplashActivity.class.getName();

    private final Observer<Boolean> kIsUserPairedWithTangibleObserver = new OnIsUserPairedWithTangibleObserver();

    private MutableLiveData<Boolean> mIsUserPairedWithTangibleLiveData = new MutableLiveData<>(false);
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

        mIsUserPairedWithTangibleLiveData.observeForever(kIsUserPairedWithTangibleObserver);

        // We want to block here so we know if we have a result or not
        mSubscriptionDisposable = TangibleUtils.getTangibleBleConnection(SplashActivity.this).subscribe(rxBleConnection -> {
            Timber.i("User is paired with Tangible");
            // Live data must be changed on the main thread
            mMainThreadHandler.post(() -> {
                mIsUserPairedWithTangibleLiveData.setValue(true);
            });
        }, throwable -> {
            Timber.e(throwable, "User is NOT paired with Tangible");
            mMainThreadHandler.post(() -> {
                mIsUserPairedWithTangibleLiveData.setValue(false);
            });
        });
    }

    private void moveToHomescreenActivity() {
        Timber.i("Moving to %s", HomescreenActivity.class.getSimpleName());
        Intent intent = new Intent(SplashActivity.this, HomescreenActivity.class);
        startActivity(intent);
        this.finish();
    }

    @Override
    protected void onStop() {
        super.onStop();

        if (mSubscriptionDisposable != null) {
            mSubscriptionDisposable.dispose();
            mSubscriptionDisposable = null;
        }

        mIsUserPairedWithTangibleLiveData.removeObserver(kIsUserPairedWithTangibleObserver);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);

        if (hasFocus) {
            ActivityUtils.hideSystemUI(this);
        }
    }

    /**
     * Mostly template code from Firebase's documentation.
     * See https://firebase.google.com/docs/auth/android/firebaseui#sign_in
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        IdpResponse loginResult = FirebaseAuthUtils.handleLoginOnActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            // If the requestCode was valid

            if (loginResult == null || loginResult.getError() != null) {
                String toastMessage = loginResult == null ? "You have cancelled sign in" : loginResult.getError().getMessage();
                Toast.makeText(SplashActivity.this, toastMessage, Toast.LENGTH_LONG).show();
            } else {
                // The login was successful
                Toast.makeText(SplashActivity.this, "Successful sign in!", Toast.LENGTH_LONG).show();
                moveToHomescreenActivity();
            }
        }
    }

    class OnIsUserPairedWithTangibleObserver implements Observer<Boolean> {

        @Override
        public void onChanged(Boolean value) {
            Lifecycle.State state = SplashActivity.this.getLifecycle().getCurrentState();

            Timber.tag("LIFECYCLE DEBUG").d("Reached lifecycle point %s with boolean value %s", state, value);

            if (state == Lifecycle.State.INITIALIZED || state == Lifecycle.State.STARTED) {
                return;
            }

            // If the user is not paired with their Tangible, get them paired first
            boolean isUserPairedWithTangible = value;
            boolean isUserLoggedIn = FirebaseAuth.getInstance().getCurrentUser() != null;

            if (isUserPairedWithTangible && isUserLoggedIn) {

                // By now, we have checked if the user is paired with their Tangible AND we have
                // checked if they are in fact signed in, we can finally send them to the
                // Homescreen activity

                Timber.i("User is logged in and has their Tangible paired");

                mMainThreadHandler.post(SplashActivity.this::moveToHomescreenActivity);

            } else if (isUserPairedWithTangible) // && !isUserLoggedIn [implied]
            {
                Timber.w("User is not logged in. Moving to Firebase Auth UI");

                // If they are not signed in (the user is null), then start the sign in UI
                mMainThreadHandler.postDelayed(() -> {

                    Toast.makeText(SplashActivity.this, "Let's get you signed in...", Toast.LENGTH_LONG).show();

                    // Create and launch sign-in intent
                    startActivityForResult(FIREBASE_AUTH_UI_INTENT, RC_SIGN_IN);

                }, TOAST_LENGTH_LONG_MS);

            } else if (isUserLoggedIn) // && !isUserPairedWithTangible
            {
                Timber.w("User is not paired with Tangible. Moving to %s", PairingActivity.class.getSimpleName());

                mMainThreadHandler.postDelayed(() -> {

                    Toast.makeText(SplashActivity.this, "Let's pair your Tangible...", Toast.LENGTH_LONG).show();

                    startActivity(new Intent(SplashActivity.this, PairingActivity.class));

                }, TOAST_LENGTH_LONG_MS);

            } else { // The user is not paired or logged in.
                // Move to the Pairing Activity which will forward the user to the
                // Firebase Auth UI as needed

                Timber.w("User is not paired with Tangible nor logged in. Moving to %s", PairingActivity.class.getSimpleName());

                // Create and launch sign-in intent
                startActivity(new Intent(SplashActivity.this, PairingActivity.class));

            }
        }
    }
}