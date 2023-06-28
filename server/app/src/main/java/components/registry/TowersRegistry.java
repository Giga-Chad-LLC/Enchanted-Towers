package components.registry;

import enchantedtowers.game_models.Tower;

import enchantedtowers.game_models.utils.Vector2;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class TowersRegistry {
    static private TowersRegistry instance = null;

    // TODO: make thread-safe
    static public TowersRegistry getInstance() {
        if (instance == null) {
            instance = new TowersRegistry();
        }
        return instance;
    }

    // instance fields
    private final List<Tower> towers = List.of(
        new Tower(0, new Vector2(39, -115)),
        new Tower(1, new Vector2(37, -118)),
        new Tower(2, new Vector2(41, -120))
    );

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
