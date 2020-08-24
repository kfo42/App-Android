package team.tangible.app.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.MutableLiveData;

import com.firebase.ui.auth.IdpResponse;

import javax.inject.Inject;
import javax.inject.Named;

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
import timber.log.Timber;

import static co.apptailor.googlesignin.RNGoogleSigninModule.RC_SIGN_IN;
import static team.tangible.app.Constants.Firebase.Authentication.FIREBASE_AUTH_UI_INTENT;
import static team.tangible.app.Constants.Toast.TOAST_LENGTH_LONG_MS;

public final class SplashActivity extends AppCompatActivity {

    private MutableLiveData<TangibleAvailabilityResult> mIsUserPairedWithTangibleLiveData =
            new MutableLiveData<>(TangibleAvailabilityResult.PENDING);
    private MutableLiveData<LoginResult> mIsUserLoggedInLiveData =
            new MutableLiveData<>(LoginResult.PENDING);

    private Disposable mSubscriptionDisposable;

    private CompositeDisposable mDisposables;

    @Inject
    @Named(Constants.Threading.MAIN_THREAD)
    Handler mMainThreadHandler;

    @Inject
    AuthenticationService mAuthenticationService;

    @Inject
    TangibleBleConnectionService mTangibleBleConnectionService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        ButterKnife.bind(this);

        ((TangibleApplication) getApplication()).getApplicationComponent().inject(this);
    }

    @Override
    protected void onStart() {
        super.onStart();

        mMainThreadHandler = new Handler(getMainLooper());

        mDisposables = new CompositeDisposable();

        // Listen for changes to the pairing and login states
        mIsUserPairedWithTangibleLiveData.observe(this, tangibleAvailabilityResult -> onLiveDataChanged());
        mIsUserLoggedInLiveData.observe(this, loginResult -> onLiveDataChanged());

        // Check if the Tangible is available
        mDisposables.add(mTangibleBleConnectionService.isTangibleAvailable().subscribe(result -> {
            mMainThreadHandler.post(() -> mIsUserPairedWithTangibleLiveData.setValue(result));
        }));

        // Check if the user is logged in
        mDisposables.add(mAuthenticationService.isUserLoggedIn().subscribe(result -> {
           mMainThreadHandler.post(() -> mIsUserLoggedInLiveData.postValue(result));
        }));
    }

    @Override
    protected void onStop() {
        super.onStop();

        if (mMainThreadHandler != null) {
            mMainThreadHandler = null;
        }

        if (mSubscriptionDisposable != null) {
            mSubscriptionDisposable.dispose();
            mSubscriptionDisposable = null;
        }

        if (mDisposables != null) {
            mDisposables.dispose();
            mDisposables = null;
        }
    }

    private void moveTo(Class<? extends Activity> destinationActivity) {
        Timber.i("Moving to %s", destinationActivity.getSimpleName());
        Intent intent = new Intent(SplashActivity.this, destinationActivity);
        startActivity(intent);
        finish();
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

        if (resultCode == RESULT_OK) {
            // If the requestCode was valid
            IdpResponse loginResult = IdpResponse.fromResultIntent(data);

            if (loginResult == null || loginResult.getError() != null) {
                String toastMessage = loginResult == null ? "You have cancelled sign in" : loginResult.getError().getMessage();
                Toast.makeText(SplashActivity.this, toastMessage, Toast.LENGTH_LONG).show();
            } else {
                // The login was successful
                Toast.makeText(SplashActivity.this, "Successful sign in!", Toast.LENGTH_LONG).show();
                moveTo(HomescreenActivity.class);
            }
        }
    }

    private void onLiveDataChanged() {
        // If the user is not paired with their Tangible, get them paired first
        TangibleAvailabilityResult isTangibleAvailable = mIsUserPairedWithTangibleLiveData.getValue();
        LoginResult isUserLoggedIn = mIsUserLoggedInLiveData.getValue();

        // If the results are pending, then wait for another change
        if (isUserLoggedIn == LoginResult.PENDING || isTangibleAvailable == TangibleAvailabilityResult.PENDING) {
            return;
        }

        if (isTangibleAvailable == TangibleAvailabilityResult.NOT_PAIRED || isTangibleAvailable == TangibleAvailabilityResult.NOT_FOUND) {
            // We want to go to the PairingActivity
            moveTo(PairingActivity.class);
            return;
        }

        // Now we know that isTangibleAvailable == TangibleAvailability.AVAILABLE
        if (BuildConfig.DEBUG && isTangibleAvailable != TangibleAvailabilityResult.AVAILABLE) {
            throw new AssertionError("Assertion failed");
        }

        if (isUserLoggedIn == LoginResult.SUCCESS) {
            // We can go to the Homescreen
            moveTo(HomescreenActivity.class);
            return;
        }

        if (BuildConfig.DEBUG && isUserLoggedIn != LoginResult.FAILURE) {
            throw new AssertionError("Assertion failed");
        }

        // If they are not signed in, then start the sign in UI
        mMainThreadHandler.postDelayed(() -> {

            Toast.makeText(SplashActivity.this, "Let's get you signed in...", Toast.LENGTH_LONG).show();

            // Create and launch sign-in intent
            startActivityForResult(FIREBASE_AUTH_UI_INTENT, RC_SIGN_IN);

        }, TOAST_LENGTH_LONG_MS);
    }
}
