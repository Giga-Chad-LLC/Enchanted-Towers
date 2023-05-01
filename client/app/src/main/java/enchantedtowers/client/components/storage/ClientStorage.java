package enchantedtowers.client.components.storage;

import java.util.Optional;

public class ClientStorage {
    static private ClientStorage instance = null;
    static public ClientStorage getInstance() {
        if (instance == null) {
            instance = new ClientStorage();
        }
        return instance;
    }

    // getter
    public Optional<Integer> getPlayerId() {
        return playerData.playerId;
    }

    public Optional<Integer> getSessionId() {
        return playerData.sessionId;
    }

    public Optional<Integer> getTowerId() {
        // TODO: return playerData.sessionId.get() that throws
        return playerData.towerId;
    }

    public Optional<Integer> getProtectionWallId() {
        return playerData.protectionWallId;
    }

    // setters
    public void setPlayerId(int playerId) {
        playerData.playerId = Optional.of(playerId);
    }

    public void setSessionId(int sessionId) {
        playerData.sessionId = Optional.of(sessionId);
    }

    public void setTowerId(int towerId) {
        playerData.towerId = Optional.of(towerId);
    }

    public void setProtectionWallId(int wallId) {
        playerData.protectionWallId = Optional.of(wallId);
    }


    private final PlayerData playerData;

    private static class PlayerData {
        public Optional<Integer> playerId = Optional.empty();
        public Optional<Integer> sessionId = Optional.empty();
        // id of tower that is being under attack of player
        public Optional<Integer> towerId  = Optional.empty();
        public Optional<Integer> protectionWallId  = Optional.empty();
    }

    private ClientStorage() {
        playerData = new PlayerData();
    }
}
