package enchantedtowers.game_models;

import enchantedtowers.game_models.utils.Vector2;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public class Tower {
    private final int towerId;
    private final Vector2 position;
    private final List<ProtectionWall> protectionWalls;
    private Optional<Integer> ownerId;
    private Optional<Instant> lastProtectionWallModificationTimestamp;

    // lock variable is used to synchronization
    private final Object lock = new Object();


    public Tower(int towerId, Vector2 position) {
        this.towerId = towerId;
        this.position = position;
        protectionWalls = List.of(
            new ProtectionWall(),
            new ProtectionWall(),
            new ProtectionWall()
        );
        ownerId = Optional.empty();
        lastProtectionWallModificationTimestamp = Optional.empty();
    }

    public int getId() {
        // towerId never changes, no need to sync
        return towerId;
    }

    public Optional<Integer> getOwnerId() {
        synchronized (lock) {
            return ownerId;
        }
    }

    public void setOwnerId(int ownerId) {
        synchronized (lock) {
            this.ownerId = Optional.of(ownerId);
        }
    }

    public boolean isAbandoned() {
        synchronized (lock) {
            return ownerId.isPresent();
        }
    }

    public Vector2 getPosition() {
        synchronized (lock) {
            return new Vector2(position.x, position.y);
        }
    }

    public void resetLastProtectionWallModificationTimestamp() {
        synchronized (lock) {
            lastProtectionWallModificationTimestamp = Optional.empty();
        }
    }

    public void setLastModificationTimestamp() {

    }
}
