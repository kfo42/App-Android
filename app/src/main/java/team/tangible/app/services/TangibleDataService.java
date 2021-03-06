package team.tangible.app.services;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import io.reactivex.Single;
import team.tangible.app.services.models.DataRecord;
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

    public Single<DataRecord<UserDocument>> getCurrentUserDocument() {
        return Single.create(emitter -> {
            User user = mAuthenticationService.getUser();
            mFirebaseFirestore.collection("users")
                    .document(user.getUserUid())
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        DocumentReference roomReference = documentSnapshot.getDocumentReference("room");

                        if (roomReference == null) {
                            emitter.onError(new NullPointerException("room reference is null"));
                            return;
                        }

                        String roomId = roomReference.getId();
                        UserDocument userDocument = new UserDocument(roomId);
                        emitter.onSuccess(new DataRecord<>(user.getUserUid(), /* resourcePath: */ null, userDocument));
                    })
                    .addOnFailureListener(exception -> {
                        Timber.e(exception);
                        emitter.onError(exception);
                    });
        });
    }

    public Single<DataRecord<RoomDocument>> getRoom(String roomId) {
        return Single.create(emitter -> {
            mFirebaseFirestore.collection("rooms")
                    .document(roomId)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        RoomDocument roomDocument = documentSnapshot.toObject(RoomDocument.class);
                        emitter.onSuccess(new DataRecord<>(documentSnapshot.getId(), documentSnapshot.getReference().getPath(), roomDocument));
                    })
                    .addOnFailureListener(exception -> {
                        Timber.e(exception);
                        emitter.onError(exception);
                    });
        });
    }


    public Single<DataRecord<RoomDocument>> getCurrentUserRoom() {
        return getCurrentUserDocument().flatMap((DataRecord<UserDocument> dataRecord) -> {
            String roomId = dataRecord.getData().getRoomId();
            return getRoom(roomId);
        });
    }
}
