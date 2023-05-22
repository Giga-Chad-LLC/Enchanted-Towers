package interactors;

import components.registry.TowersRegistry;
import enchantedtowers.common.utils.proto.common.SpellDescription;
import enchantedtowers.common.utils.proto.responses.EnchantmentResponse;
import enchantedtowers.common.utils.proto.responses.ProtectionWallResponse;
import enchantedtowers.common.utils.proto.responses.TowerResponse;
import enchantedtowers.common.utils.proto.responses.TowersAggregationResponse;
import enchantedtowers.game_models.ProtectionWall;
import enchantedtowers.game_models.Tower;
import enchantedtowers.game_models.utils.Vector2;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;


public class CreateTowersResponseInteractor {
    public TowersAggregationResponse createResponseWithAllTowers() {
        List<Tower> towers = TowersRegistry.getInstance().getTowers();
        List<TowerResponse> towerResponses = new ArrayList<>();

        for (Tower tower : towers) {
            TowerResponse response = createTowerResponse(tower);
            towerResponses.add(response);
        }

        return TowersAggregationResponse
                .newBuilder()
                .addAllTowers(towerResponses)
                .build();
    }

    public TowersAggregationResponse createResponseWithTowersWithIds(List<Integer> towerIds) {
        List<TowerResponse> towerResponses = new ArrayList<>();

        for (int id : towerIds) {
            Optional<Tower> tower = TowersRegistry.getInstance().getTowerById(id);
            if (tower.isEmpty()) {
                throw new NoSuchElementException("Tower with id " + id + " not found");
            }
            TowerResponse response = createTowerResponse(tower.get());
            towerResponses.add(response);
        }

        return TowersAggregationResponse
                .newBuilder()
                .addAllTowers(towerResponses)
                .build();
    }

    private TowerResponse createTowerResponse(Tower tower) {
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

        // adding protection walls
        List<ProtectionWallResponse> protectionWalls = createProtectionWallResponses(tower);
        towerResponseBuilder.addAllProtectionWalls(protectionWalls);

        return towerResponseBuilder.build();
    }

    private List<ProtectionWallResponse> createProtectionWallResponses(Tower tower) {
        List<ProtectionWallResponse> protectionWalls = new ArrayList<>();

        for (var wall : tower.getProtectionWalls()) {
            ProtectionWallResponse.WallState state = ProtectionWallResponse.WallState.newBuilder()
                    .setBroken(wall.isBroken())
                    .setEnchanted(wall.isEnchanted())
                    .build();

            ProtectionWallResponse.Builder responseBuilder = ProtectionWallResponse.newBuilder()
                    .setId(wall.getId())
                    .setState(state);

            // creating enchantment response if protection wall is enchanted
            if (wall.isEnchanted()) {
                EnchantmentResponse enchantment = createEnchantmentResponse(wall);
                responseBuilder.setEnchantment(enchantment);
            }

            protectionWalls.add(responseBuilder.build());
        }

        return protectionWalls;
    }

    private EnchantmentResponse createEnchantmentResponse(ProtectionWall wall) {
        List<SpellDescription> spells = new ArrayList<>();

        for (var template : wall.getEnchantment().get().getTemplateDescriptions()) {
            var offset = enchantedtowers.common.utils.proto.common.Vector2.newBuilder()
                    .setX(template.offset().x)
                    .setY(template.offset().y)
                    .build();

            SpellDescription spell = SpellDescription.newBuilder()
                    .setSpellTemplateId(template.id())
                    .setSpellType(template.spellType())
                    .setSpellTemplateOffset(offset)
                    .build();

            spells.add(spell);
        }

        return EnchantmentResponse.newBuilder().addAllSpells(spells).build();
    }

}
