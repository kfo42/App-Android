package team.tangible.app.services.models;

public class UserDocument {
    private String mRoomId;

    public UserDocument(String roomId) {
        this.mRoomId = roomId;
    }

    public String getRoomId() {
        return mRoomId;
    }
}
