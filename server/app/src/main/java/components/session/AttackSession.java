package components.session;

import enchantedtowers.common.utils.proto.responses.AttackTowerByIdResponse;
import enchantedtowers.common.utils.proto.responses.SpectateTowerAttackResponse;
import enchantedtowers.game_logic.HausdorffMetric;
import enchantedtowers.game_logic.SpellsPatternMatchingAlgorithm;
import enchantedtowers.game_models.Spell;
import enchantedtowers.game_models.SpellBook;
import enchantedtowers.game_models.utils.Utils;
import enchantedtowers.game_models.utils.Vector2;
import io.grpc.stub.StreamObserver;

import java.util.*;
import java.util.function.IntConsumer;
import java.util.logging.Logger;


public class AttackSession {
    private final int id;
    private final int attackingPlayerId;
    private final int attackedTowerId;
    private final StreamObserver<AttackTowerByIdResponse> attackerResponseObserver;
    private final IntConsumer onSessionExpiredCallback;
    private final List<Vector2> currentSpellPoints = new ArrayList<>();
    private Optional<Integer> currentSpellColorId = Optional.empty();
    private Optional<SpellsPatternMatchingAlgorithm.MatchedTemplateDescription> lastTemplateMatchDescription = Optional.empty();
    private final List<SpellsPatternMatchingAlgorithm.MatchedTemplateDescription> drawnSpellsDescriptions = new ArrayList<>();
    private final List<Spectator> spectators = new ArrayList<>();
    // this lock object is used as mutual exclusion lock
    private final Object lock = new Object();

    private static final Logger logger = Logger.getLogger(AttackSession.class.getName());

    AttackSession(int id,
                  int attackingPlayerId,
                  int attackedTowerId,
                  StreamObserver<AttackTowerByIdResponse> attackerResponseObserver,
                  IntConsumer onSessionExpiredCallback) {
        this.id = id;
        this.attackingPlayerId = attackingPlayerId;
        this.attackedTowerId = attackedTowerId;
        this.attackerResponseObserver = attackerResponseObserver;
        // TODO: create timeout event that fires `onSessionExpiredCallback`
        this.onSessionExpiredCallback = onSessionExpiredCallback;
    }

    public static class Spectator {
        private final int playerId;
        private boolean isValid;
        private final StreamObserver<SpectateTowerAttackResponse> streamObserver;

        Spectator(int playerId, StreamObserver<SpectateTowerAttackResponse> streamObserver) {
            this.playerId = playerId;
            this.isValid = true;
            this.streamObserver = streamObserver;
        }

        public int playerId() {
            return playerId;
        }

        private void invalidate() {
            isValid = false;
        }

        private boolean isValid() {
            return this.isValid;
        }

        public StreamObserver<SpectateTowerAttackResponse> streamObserver() {
            return streamObserver;
        }
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof AttackSession) {
            return this.getId() == ((AttackSession) other).getId();
        }
        return false;
    }

    public int getId() {
        synchronized (lock) {
            return id;
        }
    }

    public int getAttackingPlayerId() {
        synchronized (lock) {
            return attackingPlayerId;
        }
    }

    public int getAttackedTowerId() {
        synchronized (lock) {
            return attackedTowerId;
        }
    }

    public StreamObserver<AttackTowerByIdResponse> getAttackerResponseObserver() {
        synchronized (lock) {
            return attackerResponseObserver;
        }
    }

    /**
     * Sets <code>isValid</code> property of {@link Spectator} to <code>true</code> to indicate
     * that spectator is no longer valid and must be deleted on the next {@link AttackSession#getSpectators()} call.
     * Thus, the removal of spectator is being held lazily since it simplifies the workflow of {@link AttackSession}.
     */
    public void invalidateSpectator(int spectatorId) {
        synchronized (lock) {
            boolean invalidated = false;
            for (var spectator : spectators) {
                if (spectatorId == spectator.playerId()) {
                    spectator.invalidate();
                    invalidated = true;
                }
            }

            if (!invalidated) {
                throw new NoSuchElementException("Invalidation failed: spectator with id " + spectatorId + " not found");
            }
        }
    }

    public int getCurrentSpellColorId() {
        synchronized (lock) {
            // asserting that this method will not be used before the value assigned to the field
            assert(currentSpellColorId.isPresent());
            return currentSpellColorId.get();
        }
    }

    public List<Vector2> getCurrentSpellPoints() {
        synchronized (lock) {
            return Collections.unmodifiableList(currentSpellPoints);
        }
    }

    public boolean hasCurrentSpell() {
        synchronized (lock) {
            // TODO: explicitly set flag of the variable existence
            return this.currentSpellColorId.isPresent();
        }
    }

    public void setCurrentSpellColorId(int currentSpellColorId) {
        synchronized (lock) {
            this.currentSpellColorId = Optional.of(currentSpellColorId);
        }
    }

    public void addPointToCurrentSpell(Vector2 point) {
        synchronized (lock) {
            currentSpellPoints.add(point);
        }
    }

    public void clearCurrentDrawing() {
        synchronized (lock) {
            // clear out the current spell
            currentSpellPoints.clear();
            currentSpellColorId = Optional.empty();
        }
    }

    public List<SpellsPatternMatchingAlgorithm.MatchedTemplateDescription> getDrawnSpellsDescriptions() {
        synchronized (lock) {
            return Collections.unmodifiableList(drawnSpellsDescriptions);
        }
    }

    public void clearDrawnSpellsDescriptions() {
        synchronized (lock) {
            drawnSpellsDescriptions.clear();
        }
    }

    /**
     * This method must be called after successful {@link AttackSession#getMatchedTemplate} invocation
     */
    public void saveMatchedTemplate() {
        synchronized (lock) {
            assert(lastTemplateMatchDescription.isPresent());
            // add current template spell to the canvas history
            drawnSpellsDescriptions.add(lastTemplateMatchDescription.get());
        }
    }

    public Optional<SpellsPatternMatchingAlgorithm.MatchedTemplateDescription> getMatchedTemplate(Vector2 offset) {
        synchronized (lock) {
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
    }

    /**
     * Removes invalid spectators before returning the unmodifiable spectator list.
     */
    public List<Spectator> getSpectators() {
        synchronized (lock) {
            // remove invalidated spectators
            spectators.removeIf(spectator -> !spectator.isValid());
            return Collections.unmodifiableList(spectators);
        }
    }

    public void addSpectator(int playerId, StreamObserver<SpectateTowerAttackResponse> streamObserver) {
        synchronized (lock) {
            spectators.add(new Spectator(playerId, streamObserver));
        }
    }

    /**
     * Removes spectator by player id (if spectator exists).
     * Does not close the connection of spectator's stream observer.
     * @return either <code>Optional.empty()</code> or <code>Optional.of(removedSpectator)</code>
     */
    public Optional<Spectator> pollSpectatorById(int playerId) {
        synchronized (lock) {
            var iterator = spectators.iterator();
            Optional<Spectator> removedSpectator = Optional.empty();

            while (iterator.hasNext()) {
                Spectator spectator = iterator.next();
                if (spectator.playerId == playerId) {
                    logger.info("Spectator with id '" + playerId + "' removed from session");
                    removedSpectator = Optional.of(spectator);
                    iterator.remove();
                    break;
                }
            }

            return removedSpectator;
        }
    }
}
