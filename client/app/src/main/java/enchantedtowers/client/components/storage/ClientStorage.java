package enchantedtowers.client.components.storage;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import enchantedtowers.game_models.Spell;

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

    public Optional<Integer> getAttackSessionId() {
        return playerData.attackSessionId;
    }

    public void setPlayerId(int playerId) {
        playerData.playerId = Optional.of(playerId);
    }

    public void setAttackSessionId(int attackSessionId) {
        playerData.attackSessionId = Optional.of(attackSessionId);
    }

    private final PlayerData playerData;

    private class PlayerData {
        public Optional<Integer> playerId = Optional.empty();
        public Optional<Integer> attackSessionId = Optional.empty();
    }

    private ClientStorage() {
        playerData = new PlayerData();
    }
}

