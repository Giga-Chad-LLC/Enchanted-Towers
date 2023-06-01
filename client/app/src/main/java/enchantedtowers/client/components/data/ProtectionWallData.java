package enchantedtowers.client.components.data;

public class ProtectionWallData {
    private final int imageId;
    private final String title;

    public ProtectionWallData(int imageId, String title) {
        this.imageId = imageId;
        this.title = title;
    }


    public int getImageId() {
        return imageId;
    }

    public String getTitle() {
        return title;
    }
}
