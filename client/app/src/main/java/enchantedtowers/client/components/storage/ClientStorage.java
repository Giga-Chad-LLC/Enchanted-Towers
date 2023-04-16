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
        // TODO: return playerData.playerId.get() that throws
        return playerData.playerId;
    }

    public Optional<Integer> getSessionId() {
        // TODO: return playerData.sessionId.get() that throws
        return playerData.sessionId;
    }

    // setters
    public void setPlayerId(int playerId) {
        playerData.playerId = Optional.of(playerId);
    }

    public void setSessionId(int sessionId) {
        playerData.sessionId = Optional.of(sessionId);
    }

    private final PlayerData playerData;

    private static class PlayerData {
        public Optional<Integer> playerId = Optional.empty();
        public Optional<Integer> sessionId = Optional.empty();
    }

    private ClientStorage() {
        playerData = new PlayerData();
    }
}
