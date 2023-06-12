package enchantedtowers.game_models;

import enchantedtowers.game_models.utils.Vector2;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;


public class Tower {
    private final int towerId;
    private final Vector2 position;
    private final List<ProtectionWall> protectionWalls;
    private Optional<Integer> ownerId;
    private Optional<String> ownerUsername;
    private Optional<Instant> lastProtectionWallModificationTimestamp;
    private boolean isUnderProtectionWallsInstallation;
    // defines whether the tower has been captured recently
    private boolean isUnderCaptureLock;
    private boolean isUnderAttack;

    // lock variable is used for synchronization
    private final Object lock = new Object();
    // TODO: remove lock, use synchronized methods


    public Tower(int towerId, Vector2 position) {
        this.towerId = towerId;
        this.position = position;
        protectionWalls = List.of(
            new ProtectionWall(0),
            new ProtectionWall(1),
            new ProtectionWall(2)
        );
        ownerId = Optional.empty();
        ownerUsername = Optional.empty();
        lastProtectionWallModificationTimestamp = Optional.empty();
        isUnderProtectionWallsInstallation = false;
        isUnderCaptureLock = false;
        isUnderAttack = false;
    }

    public Tower(int towerId,
                 Vector2 position,
                 Optional<Integer> ownerId,
                 Optional<String> ownerUsername,
                 List<ProtectionWall> protectionWalls,
                 Optional<Instant> lastProtectionWallModificationTimestamp,
                 boolean isUnderProtectionWallsInstallation,
                 boolean isUnderCaptureLock,
                 boolean isUnderAttack) {
        this.towerId = towerId;
        this.position = position;
        this.protectionWalls = protectionWalls;
        this.ownerId = ownerId;
        this.ownerUsername = ownerUsername;
        this.lastProtectionWallModificationTimestamp = lastProtectionWallModificationTimestamp;
        this.isUnderProtectionWallsInstallation = isUnderProtectionWallsInstallation;
        this.isUnderCaptureLock = isUnderCaptureLock;
        this.isUnderAttack = isUnderAttack;
    }


    static public Tower of(int towerId,
                           Vector2 position,
                           Optional<Integer> ownerId,
                           Optional<String> ownerUsername,
                           List<ProtectionWall> protectionWalls,
                           Optional<Instant> lastProtectionWallModificationTimestamp,
                           boolean isUnderProtectionWallsInstallation,
                           boolean isUnderCaptureLock,
                           boolean isUnderAttack) {
        return new Tower(towerId,
                        position,
                        ownerId,
                        ownerUsername,
                        protectionWalls,
                        lastProtectionWallModificationTimestamp,
                        isUnderProtectionWallsInstallation,
                        isUnderCaptureLock,
                        isUnderAttack);
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

    public void setOwnerData(int ownerId, String username) {
        synchronized (lock) {
            // because there is a new owner who did not do any modifications yet
            this.lastProtectionWallModificationTimestamp = Optional.empty();
            this.ownerId = Optional.of(ownerId);
            this.ownerUsername = Optional.of(username);
        }
    }

    public boolean isAbandoned() {
        synchronized (lock) {
            return !ownerId.isPresent();
        }
    }

    public Vector2 getPosition() {
        synchronized (lock) {
            return new Vector2(position.x, position.y);
        }
    }

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

    public List<ProtectionWall> getProtectionWalls() {
        return Collections.unmodifiableList(protectionWalls);
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

    public ProtectionWall getEnchantedProtectionWall() {
        synchronized (lock) {
            for (ProtectionWall wall : protectionWalls) {
                if (wall.isEnchanted()) {
                    return  wall;
                }
            }
            throw new NoSuchElementException("Enchanted protection wall not found");
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

    public Optional<String> getOwnerUsername() {
        synchronized (lock) {
            return ownerUsername;
        }
    }
}
