package components.session;

import enchantedtowers.common.utils.proto.responses.SessionInfoResponse;
import io.grpc.stub.StreamObserver;

import java.util.function.IntConsumer;

public class ProtectionWallSession {
    private final int id;
    private final int playerId;
    private final int towerId;
    private final StreamObserver<SessionInfoResponse> playerResponseObserver;
    // TODO: keep track of canvas state

    private final IntConsumer onSessionExpiredCallback;
    // this lock object is used as mutual exclusion lock
    private final Object lock = new Object();

    ProtectionWallSession(int id,
                          int playerId,
                          int towerId,
                          StreamObserver<SessionInfoResponse> playerResponseObserver,
                          IntConsumer onSessionExpiredCallback) {
        this.id = id;
        this.playerId = playerId;
        this.towerId = towerId;
        this.playerResponseObserver = playerResponseObserver;
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

    public StreamObserver<SessionInfoResponse> getPlayerResponseObserver() {
        return playerResponseObserver;
    }


}
