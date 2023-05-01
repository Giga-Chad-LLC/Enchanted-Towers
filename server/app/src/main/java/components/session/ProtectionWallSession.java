package components.session;

import enchantedtowers.common.utils.proto.responses.SessionInfoResponse;
import enchantedtowers.game_logic.CanvasState;
import enchantedtowers.game_logic.TemplateDescription;
import io.grpc.stub.StreamObserver;

import java.util.Collections;
import java.util.List;
import java.util.function.IntConsumer;

public class ProtectionWallSession {
    private final int id;
    private final int playerId;
    private final int towerId;
    private final int protectionWallId;
    private final StreamObserver<SessionInfoResponse> playerResponseObserver;
    // TODO: keep track of canvas state
    private final CanvasState canvasState = new CanvasState();
    private final IntConsumer onSessionExpiredCallback;
    // this lock object is used as mutual exclusion lock
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
        // TODO: create Timeout which fires onSessionExpiredCallback
        this.onSessionExpiredCallback = onSessionExpiredCallback;
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
