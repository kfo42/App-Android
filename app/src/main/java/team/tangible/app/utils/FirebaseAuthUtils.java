package team.tangible.app.utils;

import android.content.Intent;

import com.firebase.ui.auth.IdpResponse;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.Objects;

import timber.log.Timber;

import static android.app.Activity.RESULT_OK;
import static co.apptailor.googlesignin.RNGoogleSigninModule.RC_SIGN_IN;

public class FirebaseAuthUtils {
    public static IdpResponse handleLoginOnActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == RC_SIGN_IN) {
            Timber.i("Handling Firebase authentication activity result");

            IdpResponse response = IdpResponse.fromResultIntent(data);

            if (resultCode == RESULT_OK) {
                Timber.i("Authentication is OK");

                // Successfully signed in
                FirebaseUser user = Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser());

                Timber.i("Signed in user has email %s", user.getEmail());

            } else {
                // Sign in failed. If response is null the user canceled the
                // sign-in flow using the back button. Otherwise check
                // response.getError().getErrorCode() and handle the error.
                // ...

                Timber.w("User has cancelled sign in");
            }

            return response;
        }

        return null;
    }
}
