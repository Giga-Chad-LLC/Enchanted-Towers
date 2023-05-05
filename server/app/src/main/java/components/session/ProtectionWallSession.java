package components.session;

import components.time.Timeout;
import enchantedtowers.common.utils.proto.responses.SessionInfoResponse;
import enchantedtowers.game_logic.CanvasState;
import enchantedtowers.game_models.TemplateDescription;
import io.grpc.stub.StreamObserver;

import java.util.List;
import java.util.function.IntConsumer;
import java.util.logging.Logger;

public class ProtectionWallSession {
    private static final long SESSION_EXPIRATION_TIMEOUT_MS = 10 * 1000; // 10s

    private final int id;
    private final int playerId;
    private final int towerId;
    private final int protectionWallId;
    private final StreamObserver<SessionInfoResponse> playerResponseObserver;
    // TODO: keep track of canvas state
    private final CanvasState canvasState = new CanvasState();
    private final Timeout sessionExpirationTimeout;
    private static final Logger logger = Logger.getLogger(ProtectionWallSession.class.getName());

    // this lock object is used as mutual exclusion lock
    // TODO: remove lock since all rpc calls of ProtectionWallSetupService are sync
    private final Object lock = new Object();


    ProtectionWallSession(int id,
                          int playerId,
                          int towerId,
                          int protectionWallId,
                          StreamObserver<SessionInfoResponse> playerResponseObserver,
                          IntConsumer onSessionExpiredCallback) {
        this.id = id;
        this.playerId = playerId;
        this.towerId = towerId;
        this.protectionWallId = protectionWallId;
        this.playerResponseObserver = playerResponseObserver;

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

    public int getId() {
        return id;
    }

    public int getPlayerId() {
        return playerId;
    }

    public int getTowerId() {
        return towerId;
    }

    public int getProtectionWallId() {
        return protectionWallId;
    }

    public StreamObserver<SessionInfoResponse> getPlayerResponseObserver() {
        return playerResponseObserver;
    }

    public void addTemplateToCanvasState(TemplateDescription template) {
        synchronized (lock) {
            canvasState.addTemplate(template);
        }
    }

    public void clearDrawnSpellsDescriptions() {
        synchronized (lock) {
            canvasState.clear();
        }
    }

    public List<TemplateDescription> getTemplateDescriptions() {
        synchronized (lock) {
            return canvasState.getTemplates();
        }
    }
}
