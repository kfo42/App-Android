package team.tangible.app.services;

import com.google.firebase.auth.FirebaseAuth;

import io.reactivex.Single;
import team.tangible.app.results.LoginResult;

import static team.tangible.app.results.LoginResult.*;

public class AuthenticationService {
    private final FirebaseAuth mFirebaseAuth;

    public AuthenticationService(FirebaseAuth firebaseAuth) {
        mFirebaseAuth = firebaseAuth;
    }

    public boolean isUserLoggedIn() {
        return getFirebaseUser() != null;
    }

    private FirebaseUser getFirebaseUser() {
        return mFirebaseAuth.getCurrentUser();
    }

    public User getUser() {
        if (!isUserLoggedIn()) {
            throw new AuthenticationError();
        }

        FirebaseUser firebaseUser = getFirebaseUser();

        return new User(firebaseUser.getEmail(), firebaseUser.getUid());
    }

    public static class AuthenticationError extends Error {}
}
