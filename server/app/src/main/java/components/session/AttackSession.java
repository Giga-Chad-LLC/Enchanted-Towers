package components.session;

import enchantedtowers.common.utils.proto.requests.TowerAttackRequest;
import enchantedtowers.game_logic.HausdorffMetric;
import enchantedtowers.game_logic.SpellsPatternMatchingAlgorithm;
import enchantedtowers.game_models.Spell;
import enchantedtowers.game_models.SpellBook;
import enchantedtowers.game_models.utils.Utils;
import enchantedtowers.game_models.utils.Vector2;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;


public class AttackSession {
    private final int attackingPlayerId;
    private final int attackedTowerId;

    private final List<Vector2> currentSpellPoints = new ArrayList<>();
    private Optional<Integer> currentSpellColorId = Optional.empty();
    private Optional<SpellsPatternMatchingAlgorithm.MatchedTemplateDescription> lastTemplateMatchDescription = Optional.empty();
    // TODO: add spectators

    private final List<SpellsPatternMatchingAlgorithm.MatchedTemplateDescription> drawnSpellsDescriptions = new ArrayList<>();
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


    public int getCurrentSpellColorId() {
        return currentSpellColorId.get();
    }

    public void setCurrentSpellColorId(int currentSpellColorId) {
        this.currentSpellColorId = Optional.of(currentSpellColorId);
    }

    public void clearCurrentDrawing() {
        // clear out the current spell
        currentSpellPoints.clear();
        currentSpellColorId = Optional.empty();
    }

    /**
     * This method must be called after successful getMatchedTemplate invocation
     */
    public void saveMatchedTemplate() {
        // add current template spell to the canvas history
        drawnSpellsDescriptions.add(lastTemplateMatchDescription.get());
        drawnSpellColors.add(currentSpellColorId.get());
    }

    public Optional<SpellsPatternMatchingAlgorithm.MatchedTemplateDescription> getMatchedTemplate(Vector2 offset) {
        if (Utils.isValidPath(currentSpellPoints) && currentSpellColorId.isPresent()) {
            Spell pattern = new Spell(
                Utils.getNormalizedPoints(currentSpellPoints, offset),
                offset
            );

            System.out.println("SESSION: currentSpellPoints.size=" + currentSpellPoints.size());

            Optional<SpellsPatternMatchingAlgorithm.MatchedTemplateDescription> matchedSpellDescription = SpellsPatternMatchingAlgorithm.getMatchedTemplate(
                SpellBook.getTemplates(),
                pattern,
                new HausdorffMetric()
            );

            if (matchedSpellDescription.isPresent()) {
                lastTemplateMatchDescription = matchedSpellDescription;
                return matchedSpellDescription;
            }
        }
        else {
            System.err.println("Path validity: " + Utils.isValidPath(currentSpellPoints));
            System.err.println("Current spell color present: " + currentSpellColorId.isPresent());
        }

        return Optional.empty();
    }
}
