package interactors;

import components.registry.TowersRegistry;
import enchantedtowers.common.utils.proto.responses.TowerResponse;
import enchantedtowers.common.utils.proto.responses.TowersAggregationResponse;
import enchantedtowers.game_models.Tower;
import enchantedtowers.game_models.utils.Vector2;

import java.util.ArrayList;
import java.util.List;


public class CreateTowersResponseInteractor {
    public TowersAggregationResponse execute() {
        List<Tower> towers = TowersRegistry.getInstance().getTowers();
        List<TowerResponse> towerResponses = new ArrayList<>();

        for (Tower tower : towers) {
            Vector2 position = tower.getPosition();

            var towerPosition = enchantedtowers.common.utils.proto.common.Vector2.newBuilder()
                    .setX(position.x)
                    .setY(position.y)
                    .build();

            TowerResponse.Builder towerResponseBuilder = TowerResponse.newBuilder()
                    .setTowerId(tower.getId())
                    .setPosition(towerPosition)
                    .setIsUnderProtectionWallsInstallation(tower.isUnderProtectionWallsInstallation())
                    .setIsUnderCaptureLock(tower.isUnderCaptureLock())
                    .setIsUnderAttack(tower.isUnderAttack());

            tower.getOwnerId().ifPresent(towerResponseBuilder::setOwnerId);
            tower.getLastProtectionWallModificationTimestamp().ifPresent(
                    timestamp -> towerResponseBuilder.setLastProtectionWallModificationTimestampMs(timestamp.toEpochMilli()));

            towerResponses.add(towerResponseBuilder.build());
        }

        return TowersAggregationResponse
                .newBuilder()
                .addAllTowers(towerResponses)
                .build();
    }
}
