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

    public Optional<Integer> getPlayerId() {
        return playerData.playerId;
    }

    public Optional<Integer> getTowerIdUnderAttack() {
        return playerData.towerIdUnderAttack;
    }

    public void setPlayerId(int playerId) {
        playerData.playerId = Optional.of(playerId);
    }

    public void setTowerIdUnderAttack(int towerIdUnderAttack) {
        playerData.towerIdUnderAttack = Optional.of(towerIdUnderAttack);
    }

    private final PlayerData playerData;

    private static class PlayerData {
        public Optional<Integer> playerId = Optional.empty();
        public Optional<Integer> towerIdUnderAttack = Optional.empty();
    }

    private ClientStorage() {
        playerData = new PlayerData();
    }
}

