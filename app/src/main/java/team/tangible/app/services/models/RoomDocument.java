package team.tangible.app.services.models;

public class RoomDocument {
    private String mJitsiRoom;
    public RoomDocument(String jitsiRoom) {
        this.mJitsiRoom = jitsiRoom;
    }

    public String getJitsiRoom() {
        return mJitsiRoom;
    }
}
