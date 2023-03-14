package interactors;

// game-models
import enchantedtowers.game_models.Enchantment;
import enchantedtowers.game_models.Tower;
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

    private static boolean isPlayerNearTower(double playerX, double playerY, double towerX, double towerY) {
        return (playerX - towerX) * (playerX - towerX) + (playerY - towerY) * (playerY - towerY) <= MAX_DISTANCE * MAX_DISTANCE;
    }

    public AttackTowerResponse execute(TowerAttackRequest request) {
        int towerId = request.getTowerId();
        double playerX = request.getPlayerCoordinates().getX();
        double playerY = request.getPlayerCoordinates().getY();

        EnchantmentResponseBuilder enchantmentResponseBuilder = new EnchantmentResponseBuilder();
        AttackTowerResponse.Builder responseBuilder = AttackTowerResponse.newBuilder();


        // if player is in required area near tower
        if (isPlayerNearTower(playerX, playerY, requestedTower.getX(), requestedTower.getY())) {
            for (var enchantment : enchantments) {
                EnchantmentResponse response = enchantmentResponseBuilder.buildFrom(enchantment);
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
