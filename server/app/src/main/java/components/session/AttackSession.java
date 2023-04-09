package components;

import enchantedtowers.game_logic.HausdorffMetric;
import enchantedtowers.game_logic.SpellsPatternMatchingAlgorithm;
import enchantedtowers.game_models.SpellBook;
import enchantedtowers.game_models.utils.Utils;
import java.util.ArrayList;
import java.util.List;

// game-models
import enchantedtowers.game_models.Spell;
import enchantedtowers.game_models.utils.Vector2;
// proto.requests
import enchantedtowers.common.utils.proto.requests.TowerAttackRequest;
import java.util.Optional;
import javax.swing.text.html.Option;


public class AttackSession {
    private final int attackingPlayerId;
    private final int attackedTowerId;

    private final List<Vector2> currentSpellPoints = new ArrayList<>();
    private Optional<Integer> currentSpellColorId;
    // TODO: add spectators

    private final List<Spell> drawnSpells = new ArrayList<>();
    private final List<Integer> drawnSpellColors = new ArrayList<>();
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

    public Optional<List<Vector2>> getMatchedTemplate(Vector2 offset) {
        if (Utils.isValidPath(currentSpellPoints) && currentSpellColorId.isPresent()) {
            Spell pattern = new Spell(
                Utils.getNormalizedPoints(currentSpellPoints, offset),
                offset
            );

            Optional<Spell> matchedSpell = SpellsPatternMatchingAlgorithm.getMatchedTemplate(
                SpellBook.getTemplates(),
                pattern,
                new HausdorffMetric()
            );

            if (matchedSpell.isPresent()) {
                // add current spell to the canvas history
                drawnSpells.add(matchedSpell.get());
                drawnSpellColors.add(currentSpellColorId.get());

                // clear out the current spell
                currentSpellPoints.clear();
                currentSpellColorId = Optional.empty();

                return Optional.of(matchedSpell.get().getPointsList());
            }
        }
        else {
            System.err.println("Path validity: " + Utils.isValidPath(currentSpellPoints));
            System.err.println("Current spell color present: " + currentSpellColorId.isPresent());
        }

        return Optional.empty();
    }
}
