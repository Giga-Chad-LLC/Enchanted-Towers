package enchantedtowers.client.components.data;

public class ProtectionWallData {
    private final int towerId;
    private final int protectionWallId;
    private final int imageId;
    private final String title;

    public ProtectionWallData(int towerId, int protectionWallId, int imageId, String title) {
        this.towerId = towerId;
        this.protectionWallId = protectionWallId;
        this.imageId = imageId;
        this.title = title;
    }


    public int getImageId() {
        return imageId;
    }

    public String getTitle() {
        return title;
    }

    public int getTowerId() {
        return towerId;
    }

    public int getProtectionWallId() {
        return protectionWallId;
    }
}
