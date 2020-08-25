package team.tangible.app.services;

import com.google.firebase.firestore.FirebaseFirestore;

import io.reactivex.Single;
import team.tangible.app.services.models.RoomDocument;
import team.tangible.app.services.models.User;
import team.tangible.app.services.models.UserDocument;
import timber.log.Timber;

public class TangibleDataService {
    private final FirebaseFirestore mFirebaseFirestore;
    private final AuthenticationService mAuthenticationService;

    public TangibleDataService(FirebaseFirestore firebaseFirestore, AuthenticationService authenticationService) {
        mFirebaseFirestore = firebaseFirestore;
        mAuthenticationService = authenticationService;
    }

    public Single<UserDocument> getCurrentUserDocument() {
        return Single.create(emitter -> {
            User user = mAuthenticationService.getUser();
            mFirebaseFirestore.collection("users")
                    .document(user.userUid)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        UserDocument userDocument = documentSnapshot.toObject(UserDocument.class);
                        emitter.onSuccess(userDocument);
                    })
                    .addOnFailureListener(exception -> {
                        Timber.e(exception);
                        emitter.onError(exception);
                    });
        });
    }

    public Single<RoomDocument> getRoom(String room) {
        return Single.create(emitter -> {
            mFirebaseFirestore.collection("rooms")
                    .document(room)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        RoomDocument roomDocument = documentSnapshot.toObject(RoomDocument.class);
                        emitter.onSuccess(roomDocument);
                    })
                    .addOnFailureListener(exception -> {
                        Timber.e(exception);
                        emitter.onError(exception);
                    });
        });
    }
}
