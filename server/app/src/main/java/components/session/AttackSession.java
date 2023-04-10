package components.session;

import enchantedtowers.common.utils.proto.requests.TowerAttackRequest;
import enchantedtowers.common.utils.proto.responses.SpectateTowerAttackResponse;
import enchantedtowers.game_logic.HausdorffMetric;
import enchantedtowers.game_logic.SpellsPatternMatchingAlgorithm;
import enchantedtowers.game_models.Spell;
import enchantedtowers.game_models.SpellBook;
import enchantedtowers.game_models.utils.Utils;
import enchantedtowers.game_models.utils.Vector2;
import io.grpc.stub.StreamObserver;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;


public class AttackSession {
    private final int attackingPlayerId;
    private final int attackedTowerId;

    private final List<Vector2> currentSpellPoints = new ArrayList<>();
    private Optional<Integer> currentSpellColorId = Optional.empty();
    private Optional<SpellsPatternMatchingAlgorithm.MatchedTemplateDescription> lastTemplateMatchDescription = Optional.empty();
    // TODO: add spectators

    private final List<SpellsPatternMatchingAlgorithm.MatchedTemplateDescription> drawnSpellsDescriptions = new ArrayList<>();
    private final List<Spectator> spectators = new ArrayList<>();

    private static final Logger logger = Logger.getLogger(AttackSession.class.getName());

    AttackSession(int attackingPlayerId, int attackedTowerId) {
        this.attackingPlayerId = attackingPlayerId;
        this.attackedTowerId = attackedTowerId;
    }

    public static class Spectator {
        private final int playerId;
        private final StreamObserver<SpectateTowerAttackResponse> streamObserver;

        Spectator(int playerId, StreamObserver<SpectateTowerAttackResponse> streamObserver) {
            this.playerId = playerId;
            this.streamObserver = streamObserver;
        }

        public int playerId() {
            return playerId;
        }

        public StreamObserver<SpectateTowerAttackResponse> streamObserver() {
            return streamObserver;
        }
    }

    public static AttackSession fromRequest(final TowerAttackRequest request) {
        return new AttackSession(request.getPlayerData().getPlayerId(), request.getTowerId());
    }

    public int getAttackingPlayerId() {
        return attackingPlayerId;
    }

    public int getAttackedTowerId() {
        return attackedTowerId;
    }

    public int getCurrentSpellColorId() {
        return currentSpellColorId.get();
    }

    public List<Vector2> getCurrentSpellPoints() {
        return Collections.unmodifiableList(currentSpellPoints);
    }

    public boolean hasCurrentSpell() {
        // TODO: explicitly set flag of the variable existence
        return this.currentSpellColorId.isPresent();
    }

    public void setCurrentSpellColorId(int currentSpellColorId) {
        this.currentSpellColorId = Optional.of(currentSpellColorId);
    }

    public void addPointToCurrentSpell(Vector2 point) {
        currentSpellPoints.add(point);
    }

    public void clearCurrentDrawing() {
        // clear out the current spell
        currentSpellPoints.clear();
        currentSpellColorId = Optional.empty();
    }

    public List<SpellsPatternMatchingAlgorithm.MatchedTemplateDescription> getDrawnSpellsDescriptions() {
        return Collections.unmodifiableList(drawnSpellsDescriptions);
    }

    /**
     * This method must be called after successful getMatchedTemplate invocation
     */
    public void saveMatchedTemplate() {
        // add current template spell to the canvas history
        drawnSpellsDescriptions.add(lastTemplateMatchDescription.get());
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
                currentSpellColorId.get(),
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

    public List<Spectator> getSpectators() {
        return Collections.unmodifiableList(spectators);
    }

    public void addSpectator(int playerId, StreamObserver<SpectateTowerAttackResponse> streamObserver) {
        spectators.add(new Spectator(playerId, streamObserver));
    }

    public Spectator pollSpectatorById(int playerId) {
        var iterator = spectators.iterator();
        Spectator removedSpectator = null;

        while (iterator.hasNext()) {
            Spectator spectator = iterator.next();
            if (spectator.playerId == playerId) {
                logger.info("Spectator with id '" + playerId + "' removed from session");
                removedSpectator = spectator;
                iterator.remove();
                break;
            }
        }

        return removedSpectator;
    }
}
