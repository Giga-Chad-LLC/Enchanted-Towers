package enchantedtowers.client.components.registry;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import enchantedtowers.common.utils.proto.common.Empty;
import enchantedtowers.common.utils.proto.responses.EnchantmentResponse;
import enchantedtowers.common.utils.proto.responses.ProtectionWallResponse;
import enchantedtowers.common.utils.proto.responses.TowerResponse;
import enchantedtowers.common.utils.proto.responses.TowersAggregationResponse;
import enchantedtowers.common.utils.proto.services.TowersServiceGrpc;
import enchantedtowers.common.utils.storage.ServerApiStorage;
import enchantedtowers.game_models.Enchantment;
import enchantedtowers.game_models.ProtectionWall;
import enchantedtowers.game_models.TemplateDescription;
import enchantedtowers.game_models.Tower;
import enchantedtowers.game_models.utils.Vector2;
import io.grpc.Grpc;
import io.grpc.InsecureChannelCredentials;

public class TowersRegistry {
    static private TowersRegistry instance = null;

    static public TowersRegistry getInstance() {
        if (instance == null) {
            instance = new TowersRegistry();
        }
        return instance;
    }

    private TowersRegistry() {}

    public synchronized Optional<Tower> getTowerById(int towerId) {
        for (var tower : towers) {
            if (tower.getId() == towerId) {
                return Optional.of(tower);
            }
        }
        return Optional.empty();
    }

    static private List<ProtectionWall> createProtectionWalls(TowerResponse response) {
        List<ProtectionWall> protectionWalls = new ArrayList<>();

        for (ProtectionWallResponse data : response.getProtectionWallsList()) {
            int id = data.getId();
            ProtectionWall.WallState state = new ProtectionWall.WallState(
                    data.getState().getBroken(),
                    data.getState().getEnchanted()
            );
            Optional<Enchantment> enchantment = data.hasEnchantment() ?
                    Optional.of(createEnchantment(data.getEnchantment())) : Optional.empty();

            protectionWalls.add(ProtectionWall.of(id, state, enchantment));
        }

        return protectionWalls;
    }

    static private Enchantment createEnchantment(EnchantmentResponse response) {
        List<TemplateDescription> spells = new ArrayList<>();

        for (var spell : response.getSpellsList()) {
            Vector2 offset = new Vector2(
                    spell.getSpellTemplateOffset().getX(),
                    spell.getSpellTemplateOffset().getY()
            );

            spells.add(new TemplateDescription(
                    spell.getSpellTemplateId(),
                    spell.getSpellType(),
                    offset
            ));
        }

        return new Enchantment(spells);
    }

    static private Tower createTowerFromResponse(TowerResponse response) {
        int towerId = response.getTowerId();
        Vector2 position = new Vector2(response.getPosition().getX(), response.getPosition().getY());

        Optional<Integer> ownerId = response.hasOwnerId() ? Optional.of(response.getOwnerId()) : Optional.empty();

        List<ProtectionWall> protectionWalls = createProtectionWalls(response);

        Optional<Instant> lastProtectionWallModificationTimestamp = response.hasLastProtectionWallModificationTimestampMs() ?
                Optional.of(Instant.ofEpochMilli(response.getLastProtectionWallModificationTimestampMs()))
                : Optional.empty();

        boolean isUnderProtectionWallsInstallation = response.getIsUnderProtectionWallsInstallation();
        boolean isUnderCaptureLock = response.getIsUnderCaptureLock();
        boolean isUnderAttack = response.getIsUnderAttack();

        return Tower.of(
                towerId,
                position,
                ownerId,
                protectionWalls,
                lastProtectionWallModificationTimestamp,
                isUnderProtectionWallsInstallation,
                isUnderCaptureLock,
                isUnderAttack
        );
    }

    // instance members
    private final List<Tower> towers = new ArrayList<>();

    public synchronized void createTowersFromResponse(TowersAggregationResponse response) {
        for (TowerResponse data : response.getTowersList()) {
            Tower tower = createTowerFromResponse(data);
            towers.add(tower);
        }
    }

    public synchronized void updateTowersFromResponse(TowersAggregationResponse response) {
        for (TowerResponse data : response.getTowersList()) {
            Tower updatedTower = createTowerFromResponse(data);
            towers.removeIf(tower -> tower.getId() == updatedTower.getId());
            towers.add(updatedTower);
        }
    }

    public synchronized List<Tower> getTowers() {
        return Collections.unmodifiableList(towers);
    }

}
