package interactors;

// game-models
import enchantedtowers.game_models.Enchantment;
import enchantedtowers.game_models.Tower;
// interactors
import interactors.BuildEnchantmentResponseInteractor;

// requests
import enchantedtowers.common.utils.proto.requests.TowerAttackRequest;
// responses
import enchantedtowers.common.utils.proto.responses.EnchantmentResponse;
import enchantedtowers.common.utils.proto.responses.AttackTowerResponse;
import enchantedtowers.common.utils.proto.responses.GameError;

import java.util.ArrayList;
import java.util.List;

public class AttackTowerResponseInteractor {
    private static final double MAX_DISTANCE = 10;
    private final List<Enchantment> enchantments = new ArrayList<>();
    private final Tower requestedTower = new Tower(0, 0);

    public AttackTowerResponseInteractor() {
        enchantments.add(new Enchantment(Enchantment.ElementType.EARTH));
        enchantments.add(new Enchantment(Enchantment.ElementType.AIR));
        enchantments.add(new Enchantment(Enchantment.ElementType.FIRE));
        enchantments.add(new Enchantment(Enchantment.ElementType.WATER));
    }

    public AttackTowerResponse execute(TowerAttackRequest request) {
        int towerId = request.getTowerId();
        double playerX = request.getPlayerCoordinates().getX();
        double playerY = request.getPlayerCoordinates().getY();

        BuildEnchantmentResponseInteractor enchantmentInteractor = new BuildEnchantmentResponseInteractor();
        AttackTowerResponse.Builder responseBuilder = AttackTowerResponse.newBuilder();


        // if player is in required area near tower
        if ((playerX - requestedTower.getX()) * (playerX - requestedTower.getX()) +
            (playerY - requestedTower.getY()) * (playerY - requestedTower.getY()) <= MAX_DISTANCE * MAX_DISTANCE) {
            for (var enchantment : enchantments) {
                EnchantmentResponse response = enchantmentInteractor.execute(enchantment);
                responseBuilder.getEnchantmentsBuilder().addEnchantments(response);
            }
        }
        else {
            responseBuilder.getErrorBuilder()
                    .setHasError(true)
                    .setType(GameError.ErrorType.TOWER_TOO_FAR)
                    .setMessage("Requested tower is too far away from you")
                    .build();
        }

        AttackTowerResponse response = responseBuilder.build();
        return response;
    }
}
