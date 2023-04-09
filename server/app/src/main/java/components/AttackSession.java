package components;

import java.util.ArrayList;
import java.util.List;

// game-models
import enchantedtowers.game_models.Spell;
import enchantedtowers.game_models.utils.Vector2;
// proto.requests
import enchantedtowers.common.utils.proto.requests.TowerAttackRequest;
import java.util.Optional;


public class AttackSession {
    private final int attackingPlayerId;
    private final int attackedTowerId;

    private final List<Vector2> currentSpellPoints = new ArrayList<>();
    private Optional<Integer> currentSpellColorId;
    // TODO: add spectators

    private final List<Spell> drawnSpells = new ArrayList<>();
    private final List<Integer> canvasViewer = new ArrayList<>();

    AttackSession(int attackingPlayerId, int attackedTowerId) {
        this.attackingPlayerId = attackingPlayerId;
        this.attackedTowerId = attackedTowerId;
    }

    public static AttackSession fromRequest(final TowerAttackRequest request) {
        return new AttackSession(request.getPlayerData().getPlayerId(), request.getTowerId());
    }

    public void addPointToCurrentSpell(Vector2 point) {
        currentSpellPoints.add(point);
    }

    public int getAttackingPlayerId() {
        return attackingPlayerId;
    }

    public void setCurrentSpellColorId(int currentSpellColorId) {
        this.currentSpellColorId = Optional.of(currentSpellColorId);
    }
}
