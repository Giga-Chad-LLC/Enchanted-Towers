package enchantedtowers.client.components.registry;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import enchantedtowers.common.utils.proto.common.Empty;
import enchantedtowers.common.utils.proto.responses.TowerResponse;
import enchantedtowers.common.utils.proto.responses.TowersAggregationResponse;
import enchantedtowers.common.utils.proto.services.TowersServiceGrpc;
import enchantedtowers.common.utils.storage.ServerApiStorage;
import enchantedtowers.game_models.Tower;
import enchantedtowers.game_models.utils.Vector2;
import io.grpc.Grpc;
import io.grpc.InsecureChannelCredentials;

public class TowersRegistry {
    static private TowersRegistry instance = null;
    private TowersServiceGrpc.TowersServiceBlockingStub blockingStub;

    static public TowersRegistry getInstance() {
        if (instance == null) {
            instance = new TowersRegistry();
        }
        return instance;
    }

    private TowersRegistry() {
        // creating client stub
        String host   = ServerApiStorage.getInstance().getClientHost();
        int port      = ServerApiStorage.getInstance().getPort();
        String target = host + ":" + port;

        blockingStub = TowersServiceGrpc.newBlockingStub(
                Grpc.newChannelBuilder(target, InsecureChannelCredentials.create()).build());

        TowersAggregationResponse response = blockingStub.getTowers(Empty.newBuilder().build());

        // TODO: create Tower.of(TowerResponse)
        for (TowerResponse data : response.getTowersList()) {
            int towerId = data.getTowerId();
            Vector2 position = new Vector2(data.getPosition().getX(), data.getPosition().getY());
            Optional<Integer> ownerId = data.hasOwnerId() ? Optional.of(data.getOwnerId()) : Optional.empty();
            Optional<Instant> lastProtectionWallModificationTimestamp =
                    data.hasLastProtectionWallModificationTimestampMs() ?
                            Optional.of(Instant.ofEpochMilli(data.getLastProtectionWallModificationTimestampMs()))
                            : Optional.empty();
            boolean isUnderProtectionWallsInstallation = data.getIsUnderProtectionWallsInstallation();
            boolean isUnderCaptureLock = data.getIsUnderCaptureLock();
            boolean isUnderAttack = data.getIsUnderAttack();

            towers.add(Tower.of(
                    towerId,
                    position,
                    ownerId,
                    lastProtectionWallModificationTimestamp,
                    isUnderProtectionWallsInstallation,
                    isUnderCaptureLock,
                    isUnderAttack
            ));
        }
    }

    // instance fields
    private final List<Tower> towers = new ArrayList<>();

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
