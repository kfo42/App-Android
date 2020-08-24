package team.tangible.app.services;

import com.google.firebase.auth.FirebaseAuth;

import io.reactivex.Single;
import team.tangible.app.models.LoginResult;

import static team.tangible.app.models.LoginResult.*;

public class AuthenticationService {
    private final FirebaseAuth mFirebaseAuth;

    public AuthenticationService(FirebaseAuth firebaseAuth) {
        mFirebaseAuth = firebaseAuth;
    }

    public Single<LoginResult> isUserLoggedIn() {
        return Single.just(mFirebaseAuth.getCurrentUser() != null ? SUCCESS : FAILURE);
    }
}
