package team.tangible.app.services.models;

public class User {
    private final String mIdentifier;
    private final String mUserUid;

    public User(String identifier, String userUid) {
        this.mIdentifier = identifier;
        this.mUserUid = userUid;
    }

    public String getIdentifier() {
        return mIdentifier;
    }

    public String getUserUid() {
        return mUserUid;
    }
}
