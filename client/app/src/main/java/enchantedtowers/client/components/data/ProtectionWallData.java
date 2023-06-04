package enchantedtowers.client.components.data;

public class ProtectionWallData {
    private final int towerId;
    private final int protectionWallId;
    private final int imageId;

    public ProtectionWallData(int towerId, int protectionWallId, int imageId) {
        this.towerId = towerId;
        this.protectionWallId = protectionWallId;
        this.imageId = imageId;
    }


    public int getImageId() {
        return imageId;
    }

    public int getTowerId() {
        return towerId;
    }

    public int getProtectionWallId() {
        return protectionWallId;
    }
}
