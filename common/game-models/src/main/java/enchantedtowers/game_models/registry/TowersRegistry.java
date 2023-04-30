package enchantedtowers.game_models.registry;

import enchantedtowers.game_models.Tower;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class TowersRegistry {
    static private TowersRegistry instance = null;

    static public TowersRegistry getInstance() {
        if (instance == null) {
            instance = new TowersRegistry();
        }
        return instance;
    }

    // instance fields
    List<Tower> towers = new ArrayList<>();

    private TowersRegistry() {}

    public List<Tower> getTowers() {
        // TODO: make DB query to get towers
        return Collections.unmodifiableList(towers);
    }

    public Optional<Tower> getTowerById(int towerId) {
        // TODO: fetch tower from DB and update its state
        for (var tower : towers) {
            if (tower.getId() == towerId) {
                return Optional.of(tower);
            }
        }
        return Optional.empty();
    }
}
