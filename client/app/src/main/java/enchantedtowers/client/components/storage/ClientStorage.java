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

    public Optional<Integer> getTowerIdUnderAttack() {
        return playerData.towerIdUnderAttack;
    }

    public Optional<Integer> getTowerIdUnderSpectate() {
        return playerData.towerIdUnderSpectate;
    }

    // setters
    public void setPlayerId(int playerId) {
        playerData.playerId = Optional.of(playerId);
    }

    public void setTowerIdUnderAttack(int towerIdUnderAttack) {
        playerData.towerIdUnderAttack = Optional.of(towerIdUnderAttack);
    }

    public void setTowerIdUnderSpectate(int towerIdUnderSpectate) {
        playerData.towerIdUnderSpectate = Optional.of(towerIdUnderSpectate);
    }

    private final PlayerData playerData;

    private static class PlayerData {
        public Optional<Integer> playerId = Optional.empty();
        public Optional<Integer> towerIdUnderAttack = Optional.empty();
        public Optional<Integer> towerIdUnderSpectate = Optional.empty();
    }

    private ClientStorage() {
        playerData = new PlayerData();
    }
}

