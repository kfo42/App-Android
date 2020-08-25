package team.tangible.app.services;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.concurrent.Callable;

import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.SingleSource;
import io.reactivex.functions.Function;
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

    public Single<RoomDocument> getRoom(String roomId) {
        return Single.create(emitter -> {
            mFirebaseFirestore.collection("rooms")
                    .document(roomId)
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


    public Single<RoomDocument> getCurrentUserRoom() {
        return getCurrentUserDocument().flatMap(userDocument -> getRoom(userDocument.room.getId()));
    }
}
