package enchantedtowers.game_models;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

// game_models
import enchantedtowers.game_models.ProtectionWall;
// game_models.utils
import enchantedtowers.game_models.utils.Point;



public class Tower {
    private final int towerId;
    private final List<ProtectionWall> protectionWalls;
    private final TowerCharacteristics characteristics;
    private final OwningState owningState;
    private final Point position;


    public enum TowerType {
        CASTLE,
        FORTRESS,
        HUT,
    }

    private record TowerCharacteristics(int protectionWallsCount, TowerType type) {}

    private record OwningState(boolean isOwned, Integer ownerId, Integer clanId, Instant capturedAt) {}

    public Tower(int towerId, TowerType type, Point position) {
        this.towerId = towerId;

        int protectionWallsCount = switch (type) {
            case CASTLE -> 3;
            case FORTRESS -> 2;
            case HUT -> 1;
        };

        this.protectionWalls = new ArrayList<>(protectionWallsCount);
        for (int i = 0; i < protectionWallsCount; ++i) {
            protectionWalls.add(new ProtectionWall());
        }

        this.characteristics = new TowerCharacteristics(protectionWallsCount, type);
        this.owningState = new OwningState(false, null, null, null);
        this.position = position;
    }

    public int getId() {
        return towerId;
    }

    public Point getPosition() {
        return position;
    }
}
