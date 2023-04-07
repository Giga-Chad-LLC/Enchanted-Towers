package components;

import java.util.ArrayList;
import java.util.List;

// game-models
import enchantedtowers.game_models.Spell;
import enchantedtowers.game_models.utils.Vector2;
// proto.requests
import enchantedtowers.common.utils.proto.requests.TowerAttackRequest;



public class AttackSession {
    private final int attackingPlayerId;
    private final int attackedTowerId;

    private final List<Vector2> currentSpellPoints = new ArrayList<>();
    private final List<Spell> drawnSpells = new ArrayList<>();
    private final List<Integer> canvasViewer = new ArrayList<>();

    AttackSession(int attackingPlayerId, int attackedTowerId) {
        this.attackingPlayerId = attackingPlayerId;
        this.attackedTowerId = attackedTowerId;
    }

    public static AttackSession fromRequest(final TowerAttackRequest request) {
        return new AttackSession(request.getPlayerData().getPlayerId(), request.getTowerId());
    }

    public int getAttackingPlayerId() {
        return attackingPlayerId;
    }
}
