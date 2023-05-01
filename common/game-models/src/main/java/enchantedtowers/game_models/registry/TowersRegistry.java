package enchantedtowers.game_models.registry;

import enchantedtowers.game_models.Tower;

import enchantedtowers.game_models.utils.Vector2;
import java.util.ArrayList;
import java.util.Arrays;
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
    List<Tower> towers = List.of(
        new Tower(0, new Vector2(10, 10)),
        new Tower(1, new Vector2(20, 20)),
        new Tower(2, new Vector2(50, 50))
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
