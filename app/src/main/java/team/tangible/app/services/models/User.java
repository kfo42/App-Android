package team.tangible.app.services.models;

public class User {
    public final String identifier;
    public final String userUid;

    public User(String identifier, String userUid) {
        this.identifier = identifier;
        this.userUid = userUid;
    }
}
