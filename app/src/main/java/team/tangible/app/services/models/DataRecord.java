package team.tangible.app.services.models;

public class DataRecord<TData> {
    private final String mId;
    private final String mResourcePath;
    private final TData mData;

    public DataRecord(String id, String resourcePath, TData data) {
        this.mId = id;
        this.mResourcePath = resourcePath;
        this.mData = data;
    }

    public String getId() {
        return mId;
    }

    public String getResourcePath() {
        return mResourcePath;
    }

    public TData getData() {
        return mData;
    }
}
