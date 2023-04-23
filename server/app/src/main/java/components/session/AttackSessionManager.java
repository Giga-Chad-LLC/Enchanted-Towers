package components.session;

import enchantedtowers.common.utils.proto.responses.AttackTowerByIdResponse;
import io.grpc.stub.StreamObserver;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.function.IntConsumer;

public class AttackSessionManager {
   // towerId -> sessions
   private final Map<Integer, List<AttackSession>> sessions = new TreeMap<>();
   private int CURRENT_SESSION_ID = 2;

   public AttackSession createAttackSession(int playerId,
                                  int towerId,
                                  StreamObserver<AttackTowerByIdResponse> attackerResponseObserver,
                                  IntConsumer onSessionExpiredCallback) {
      synchronized (sessions) {
         if (!sessions.containsKey(towerId)) {
            sessions.put(towerId, new ArrayList<>());
         }

         int sessionId = CURRENT_SESSION_ID++;

         AttackSession session = new AttackSession(sessionId, playerId, towerId, attackerResponseObserver, onSessionExpiredCallback);
         sessions.get(towerId).add(session);

         return session;
      }
   }

   public void remove(AttackSession session) {
      synchronized (sessions) {
         for (var sessionList : sessions.values()) {
            if (sessionList.contains(session)) {
               sessionList.remove(session);
               return;
            }
         }
      }
   }

   /**
    * @return <code>true</code> if there exists attack session that is associated with player with provided id.
    * Otherwise, returns <code>false</code>.
    */
   public boolean hasSessionAssociatedWithPlayerId(int playerId) {
      synchronized (sessions) {
         for (var sessionList : sessions.values()) {
            for (var session : sessionList) {
               if (playerId == session.getAttackingPlayerId()) {
                  return true;
               }
            }
         }
         return false;
      }
   }

   /**
    *
    * @return <code>true</code> if there exists an attack session that has <code>playerId</code> as a spectator.
    * Otherwise, returns <code>false</code>.
    */
   public boolean isPlayerInSpectatingMode(int playerId) {
      synchronized (sessions) {
         for (var sessionList : sessions.values()) {
            for (var session : sessionList) {
               for (var spectator : session.getSpectators()) {
                  if (playerId == spectator.playerId()) {
                     return true;
                  }
               }
            }
         }
         return false;
      }
   }

   public Optional<AttackSession> getSessionById(int sessionId) {
      synchronized (sessions) {
         for (var sessionList : sessions.values()) {
            for (var session : sessionList) {
               if (sessionId == session.getId()) {
                  return Optional.of(session);
               }
            }
         }
         return Optional.empty();
      }
   }

   public Optional<AttackSession> getAnyAttackSessionByTowerId(int towerId) {
      synchronized (sessions) {
         if (sessions.containsKey(towerId)) {
            var sessionList = sessions.get(towerId);

            // returning first session, if exists
            for (var session : sessionList) {
               return Optional.of(session);
            }
         }
         return Optional.empty();
      }
   }
}
