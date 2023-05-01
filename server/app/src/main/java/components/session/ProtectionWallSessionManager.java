package components.session;

import enchantedtowers.common.utils.proto.responses.SessionInfoResponse;
import io.grpc.stub.StreamObserver;

import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.function.IntConsumer;

public class ProtectionWallSessionManager {
    // towerId -> ProtectionWallSession
    private final Map<Integer, ProtectionWallSession> sessions = new HashMap<>();
    private int CURRENT_SESSION_ID = 0;
    private final Object lock = new Object();


    public ProtectionWallSession createSession(int playerId,
                                               int towerId,
                                               int protectionWallId,
                                               StreamObserver<SessionInfoResponse> playerResponseObserver,
                                               IntConsumer onSessionExpiredCallback) {
        synchronized (lock) {
            var session = new ProtectionWallSession(
                        CURRENT_SESSION_ID++,
                        playerId,
                        towerId,
                        protectionWallId,
                        playerResponseObserver,
                        onSessionExpiredCallback);

            sessions.put(towerId, session);
            return session;
        }
    }

    // TODO: before removing session, cancel timeout of SessionExpiredCallback
    public void remove(ProtectionWallSession session) {
        synchronized (lock) {
            boolean removed = sessions.remove(session.getTowerId(), session);
            if (!removed) {
                throw new NoSuchElementException("Attack session with id " + session.getId() + " not found");
            }
        }
    }

    public boolean hasSessionAssociatedWithPlayerId(int playerId) {
        synchronized (lock) {
            for (var session : sessions.values()) {
                if (playerId == session.getPlayerId()) {
                    return true;
                }
            }
            return false;
        }
    }

    public Optional<ProtectionWallSession> getSessionById(int sessionId) {
        synchronized (lock) {
            for (var session : sessions.values()) {
                if (sessionId == session.getId()) {
                    return Optional.of(session);
                }
            }
            return Optional.empty();
        }
    }


}
