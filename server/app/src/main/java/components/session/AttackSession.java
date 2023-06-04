package components.session;

import components.time.Timeout;
import enchantedtowers.common.utils.proto.common.SpellType;
import enchantedtowers.common.utils.proto.responses.SessionInfoResponse;
import enchantedtowers.common.utils.proto.responses.SpectateTowerAttackResponse;
import enchantedtowers.game_logic.canvas.CanvasState;
import enchantedtowers.game_models.TemplateDescription;
import enchantedtowers.game_logic.canvas.SpellDrawingDescription;
import enchantedtowers.game_models.utils.Vector2;
import io.grpc.stub.StreamObserver;

import java.util.*;
import java.util.function.IntConsumer;
import java.util.logging.Logger;


/**
 * <p>Represents tower attack session by storing the player's {@link StreamObserver} and spectators' {@link StreamObserver}.</p>
 * <p>Caller must provide thread-safe execution of the methods.</p>
 */
public class AttackSession {
    private static final long SESSION_EXPIRATION_TIMEOUT_MS = 10 * 1000; // 10s

    private final int id;
    private final int attackingPlayerId;
    private final int attackedTowerId;
    private final int protectionWallId;
    private final StreamObserver<SessionInfoResponse> attackerResponseObserver;
    private final Timeout sessionExpirationTimeout;
    private final CanvasState canvasState = new CanvasState();
    private final SpellDrawingDescription currentSpellDescription = new SpellDrawingDescription();
    private final List<Spectator> spectators = new ArrayList<>();

    private static final Logger logger = Logger.getLogger(AttackSession.class.getName());

    AttackSession(int id,
                  int attackingPlayerId,
                  int attackedTowerId,
                  int protectionWallId,
                  StreamObserver<SessionInfoResponse> attackerResponseObserver,
                  IntConsumer onSessionExpiredCallback) {
        this.id = id;
        this.attackingPlayerId = attackingPlayerId;
        this.attackedTowerId = attackedTowerId;
        this.protectionWallId = protectionWallId;
        this.attackerResponseObserver = attackerResponseObserver;

        // timeout event that fires onSessionExpiredCallback
        logger.info("starting session expiration timeout (session id " + id + ")");
        this.sessionExpirationTimeout = new Timeout(
                SESSION_EXPIRATION_TIMEOUT_MS,
                () -> onSessionExpiredCallback.accept(this.id)
        );
    }

    public void cancelExpirationTimeout() {
        this.sessionExpirationTimeout.cancel();
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
        return id;
    }

    public int getProtectionWallId() {
        return protectionWallId;
    }

    public int getAttackingPlayerId() {
        return attackingPlayerId;
    }

    public int getAttackedTowerId() {
        return attackedTowerId;
    }

    public StreamObserver<SessionInfoResponse> getAttackerResponseObserver() {
        return attackerResponseObserver;
    }

    /**
     * Sets <code>isValid</code> property of {@link Spectator} to <code>true</code> to indicate
     * that spectator is no longer valid and must be deleted on the next {@link AttackSession#getSpectators()} call.
     * Thus, the removal of spectator is being held lazily since it simplifies the workflow of {@link AttackSession}.
     */
    public void invalidateSpectator(int spectatorId) {
        boolean invalidated = false;
        for (var spectator : spectators) {
            if (spectatorId == spectator.playerId()) {
                spectator.invalidate();
                invalidated = true;
            }
        }

        if (!invalidated) {
            // Note: spectator might have changed the attack session
            logger.info("Invalidation failed: spectator with id " + spectatorId +
                    " not found (spectator might have changed the attack session)");
        }
    }

    public SpellType getCurrentSpellType() {
        // asserting that this method will not be used before the value assigned to the field
        return currentSpellDescription.getSpellType();
    }

    public List<Vector2> getCurrentSpellPoints() {
        return currentSpellDescription.getPoints();
    }

    public boolean hasCurrentSpell() {
        return !currentSpellDescription.getPoints().isEmpty();
    }

    public void setCurrentSpellType(SpellType currentSpellType) {
        currentSpellDescription.setSpellType(currentSpellType);
    }

    public void addPointToCurrentSpell(Vector2 point) {
        currentSpellDescription.addPoint(point);
    }

    public void addTemplateToCanvasState(TemplateDescription template) {
        canvasState.addTemplate(template);
    }

    public void clearCurrentDrawing() {
        // clear out the current spell
        currentSpellDescription.reset();
    }

    public List<TemplateDescription> getDrawnSpellsDescriptions() {
        return canvasState.getTemplates();
    }

    public void clearDrawnSpellsDescriptions() {
        canvasState.clear();
    }

    /**
     * Removes invalid spectators before returning the unmodifiable spectator list.
     */
    public List<Spectator> getSpectators() {
        // remove invalidated spectators
        spectators.removeIf(spectator -> !spectator.isValid());
        return Collections.unmodifiableList(spectators);
    }

    public void addSpectator(int playerId, StreamObserver<SpectateTowerAttackResponse> streamObserver) {
        spectators.add(new Spectator(playerId, streamObserver));
    }

    /**
     * Removes spectator by player id (if spectator exists).
     * Does not close the connection of spectator's stream observer.
     * @return either <code>Optional.empty()</code> or <code>Optional.of(removedSpectator)</code>
     */
    public Optional<Spectator> pollSpectatorById(int playerId) {
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
