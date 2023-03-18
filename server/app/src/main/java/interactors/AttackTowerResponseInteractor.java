package interactors;

// builders
import builders.EnchantmentModelBuilder;
// game-models
import enchantedtowers.game_models.Enchantment;
import enchantedtowers.game_models.Tower;
import enchantedtowers.game_models.Spell;
// game-models.utils
import enchantedtowers.game_models.utils.Point;
// requests
import enchantedtowers.common.utils.proto.requests.TowerAttackRequest;
// responses
import enchantedtowers.common.utils.proto.responses.AttackTowerResponse;
import enchantedtowers.common.utils.proto.responses.GameError;
// proto/common
import enchantedtowers.common.utils.proto.common.EnchantmentModel;


import java.util.ArrayList;
import java.util.List;

public class AttackTowerResponseInteractor {
    private static final double MAX_DISTANCE = 10;
    private final List<enchantedtowers.game_models.Enchantment> enchantments = new ArrayList<>();
    private final Tower requestedTower = new Tower(1, Tower.TowerType.CASTLE, new Point(0, 0));

    public AttackTowerResponseInteractor() {
        Spell airSpell = new Spell(Spell.ElementType.AIR, Spell.SpellForm.CIRCLE, new Point());
        Spell fireSpell = new Spell(Spell.ElementType.FIRE, Spell.SpellForm.CIRCLE, new Point());
        Spell earthSpell = new Spell(Spell.ElementType.EARTH, Spell.SpellForm.ELLIPSE, new Point());

        enchantments.add(new Enchantment(10, List.of(airSpell, airSpell, fireSpell)));
        enchantments.add(new Enchantment(32, List.of(fireSpell, fireSpell, earthSpell)));
        enchantments.add(new Enchantment(1, List.of(earthSpell, airSpell)));
        enchantments.add(new Enchantment(11, List.of(airSpell, fireSpell)));
    }

    private static boolean isPlayerNearTower(double playerX, double playerY, double towerX, double towerY) {
        return (playerX - towerX) * (playerX - towerX) + (playerY - towerY) * (playerY - towerY) <= MAX_DISTANCE * MAX_DISTANCE;
    }

    public AttackTowerResponse execute(TowerAttackRequest request) {
        int towerId = request.getTowerId();
        double playerX = request.getPlayerCoordinates().getLocation().getX();
        double playerY = request.getPlayerCoordinates().getLocation().getY();

        EnchantmentModelBuilder enchantmentModelBuilder = new EnchantmentModelBuilder();
        AttackTowerResponse.Builder responseBuilder = AttackTowerResponse.newBuilder();


        // if player is in required area near tower
        if (isPlayerNearTower(playerX, playerY, requestedTower.getPosition().getX(), requestedTower.getPosition().getY())) {
            for (var enchantment : enchantments) {
                EnchantmentModel model = enchantmentModelBuilder.buildFrom(enchantment);
                responseBuilder.getEnchantmentsBuilder().addEnchantments(model);
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
