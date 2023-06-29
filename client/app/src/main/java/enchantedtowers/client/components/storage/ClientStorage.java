package enchantedtowers.client.components.storage;

import androidx.annotation.NonNull;

import java.util.Optional;

public class ClientStorage {
    static private ClientStorage instance = null;
    static public ClientStorage getInstance() {
        if (instance == null) {
            instance = new ClientStorage();
        }
        return instance;
    }

    // getters
    public Optional<String> getJWTToken() {
        return playerData.jwtToken;
    }

    public Optional<String> getGameSessionToken() {
        return playerData.gameSessionToken;
    }

    public Optional<Integer> getPlayerId() {
        return playerData.playerId;
    }

    public Optional<String> getUsername() {
        return playerData.username;
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
    public void setJWTToken(String jwt) {
        playerData.jwtToken = Optional.of(jwt);
    }

    public void setGameSessionToken(String token) {
        playerData.gameSessionToken = Optional.of(token);
    }

    public void resetGameSessionToken() {
        playerData.gameSessionToken = Optional.empty();
    }

    public void setPlayerId(int playerId) {
        playerData.playerId = Optional.of(playerId);
    }

    public void setUsername(@NonNull String username) {
        playerData.username = Optional.of(username);
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
        public Optional<String> jwtToken = Optional.empty();
        public Optional<String> gameSessionToken = Optional.empty();
        public Optional<Integer> playerId = Optional.empty();
        public Optional<String> username = Optional.empty();
        public Optional<Integer> sessionId = Optional.empty();
        // id of tower that is being under attack of player
        public Optional<Integer> towerId  = Optional.empty();
        public Optional<Integer> protectionWallId  = Optional.empty();
    }

    private ClientStorage() {
        playerData = new PlayerData();
    }
}
