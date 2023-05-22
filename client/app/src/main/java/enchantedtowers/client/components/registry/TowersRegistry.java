package enchantedtowers.client.components.registry;

import java.time.Instant;
import java.util.ArrayList;
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

    // instance members
    private final List<Tower> towers = new ArrayList<>();

    public void createTowersFromResponse(TowersAggregationResponse response) {
        for (TowerResponse data : response.getTowersList()) {
            int towerId = data.getTowerId();
            Vector2 position = new Vector2(data.getPosition().getX(), data.getPosition().getY());

            Optional<Integer> ownerId = data.hasOwnerId() ? Optional.of(data.getOwnerId()) : Optional.empty();

            List<ProtectionWall> protectionWalls = createProtectionWalls(data);

            Optional<Instant> lastProtectionWallModificationTimestamp = data.hasLastProtectionWallModificationTimestampMs() ?
                    Optional.of(Instant.ofEpochMilli(data.getLastProtectionWallModificationTimestampMs()))
                    : Optional.empty();

            boolean isUnderProtectionWallsInstallation = data.getIsUnderProtectionWallsInstallation();
            boolean isUnderCaptureLock = data.getIsUnderCaptureLock();
            boolean isUnderAttack = data.getIsUnderAttack();

            towers.add(Tower.of(
                    towerId,
                    position,
                    ownerId,
                    protectionWalls,
                    lastProtectionWallModificationTimestamp,
                    isUnderProtectionWallsInstallation,
                    isUnderCaptureLock,
                    isUnderAttack
            ));
        }
    }

    public List<Tower> getTowers() {
        return towers;
    }

    public Optional<Tower> getTowerById(int towerId) {
        for (var tower : towers) {
            if (tower.getId() == towerId) {
                return Optional.of(tower);
            }
        }
        return Optional.empty();
    }
}
