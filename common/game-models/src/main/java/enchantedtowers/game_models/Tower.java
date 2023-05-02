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
    private boolean isUnderProtectionWallsInstallation;
    // defines whether the tower has been captured recently and whether owner can install enchantments on unprotected walls
    private boolean isUnderCaptureLock;
    private boolean isUnderAttack;


    // lock variable is used to synchronization
    private final Object lock;


    public Tower(int towerId, Vector2 position) {
        this.towerId = towerId;
        this.position = position;
        protectionWalls = List.of(
            new ProtectionWall(0),
            new ProtectionWall(1),
            new ProtectionWall(2)
        );
        ownerId = Optional.empty();
        lastProtectionWallModificationTimestamp = Optional.empty();
        isUnderProtectionWallsInstallation = false;
        isUnderCaptureLock = false;
        isUnderAttack = false;
        lock = new Object();
    }

    public int getId() {
        // towerId never changes, no need to sync
        return towerId;
    }

    public boolean isProtected() {
        synchronized (lock) {
            boolean hasProtection = false;
            for (var wall : protectionWalls) {
                hasProtection |= wall.isEnchanted();
            }
            return hasProtection;
        }
    }

    public Optional<Integer> getOwnerId() {
        synchronized (lock) {
            return ownerId;
        }
    }

    public void setOwnerId(int ownerId) {
        synchronized (lock) {
            // because there is a new owner who did not do any modifications yet
            this.lastProtectionWallModificationTimestamp = Optional.empty();
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

    /*public void resetLastProtectionWallModificationTimestamp() {
        synchronized (lock) {
            lastProtectionWallModificationTimestamp = Optional.empty();
        }
    }*/

    public void setLastProtectionWallModificationTimestamp(Instant timestamp) {
        synchronized (lock) {
            lastProtectionWallModificationTimestamp = Optional.of(timestamp);
        }
    }

    public boolean isUnderProtectionWallsInstallation() {
        synchronized (lock) {
            return isUnderProtectionWallsInstallation;
        }
    }

    public void setUnderProtectionWallsInstallation(boolean value) {
        synchronized (lock) {
            isUnderProtectionWallsInstallation = value;
        }
    }

    public Optional<Instant> getLastProtectionWallModificationTimestamp() {
        synchronized (lock) {
            return lastProtectionWallModificationTimestamp;
        }
    }

    public Optional<ProtectionWall> getProtectionWallById(int protectionWallId) {
        synchronized (lock) {
            for (ProtectionWall wall : protectionWalls) {
                if (protectionWallId == wall.getId()) {
                    return Optional.of(wall);
                }
            }
            return Optional.empty();
        }
    }

    public boolean hasProtectionWallWithId(int protectionWallId) {
        synchronized (lock) {
            for (ProtectionWall wall : protectionWalls) {
                if (protectionWallId == wall.getId()) {
                    return true;
                }
            }
            return false;
        }

    }

    public boolean isUnderCaptureLock() {
        synchronized (lock) {
            return isUnderCaptureLock;
        }
    }

    public void setUnderCaptureLock(boolean underCaptureLock) {
        synchronized (lock) {
            isUnderCaptureLock = underCaptureLock;

        }
    }

    public boolean isUnderAttack() {
        synchronized (lock) {
            return isUnderAttack;
        }
    }

    public void setUnderAttack(boolean underAttack) {
        synchronized (lock) {
            isUnderAttack = underAttack;
        }
    }
}
