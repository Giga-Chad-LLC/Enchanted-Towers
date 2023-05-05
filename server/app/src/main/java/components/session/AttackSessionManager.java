package components.session;

import enchantedtowers.common.utils.proto.responses.SessionInfoResponse;
import io.grpc.stub.StreamObserver;

import java.util.*;
import java.util.function.IntConsumer;

/**
 * <p>{@link AttackSessionManager} provides convenient API of storing/removing/searching {@link AttackSession}.</p>
 * <p>Caller must provide thread-safe execution of the class' methods.</p>
 */
public class AttackSessionManager {
   // towerId -> sessions
   private final Map<Integer, List<AttackSession>> sessions = new TreeMap<>();
   private int CURRENT_SESSION_ID = 0;

   public AttackSession createAttackSession(int playerId,
                                  int towerId,
                                  int protectionWallId,
                                  StreamObserver<SessionInfoResponse> attackerResponseObserver,
                                  IntConsumer onSessionExpiredCallback) {
      if (!sessions.containsKey(towerId)) {
         sessions.put(towerId, new ArrayList<>());
      }

      int sessionId = CURRENT_SESSION_ID++;

      AttackSession session = new AttackSession(
              sessionId, playerId, towerId, protectionWallId, attackerResponseObserver, onSessionExpiredCallback);

      sessions.get(towerId).add(session);

      return session;
   }

   public void remove(AttackSession session) {
      for (var sessionList : sessions.values()) {
         if (sessionList.contains(session)) {
            // cancelling timeout callback
            session.cancelExpirationTimeout();
            sessionList.remove(session);
            return;
         }
      }
      throw new NoSuchElementException("Attack session with id " + session.getId() + " not found");
   }

   public AttackSession getKthNeighbourOfSession(int towerId, AttackSession session, int k) {
      List<AttackSession> allSessionsList = sessions.get(towerId);
      int index = allSessionsList.indexOf(session);

      if (index == -1) {
         throw new NoSuchElementException("Attack session with id " + session.getId() + " not found in session manager's registry");
      }

      int neighbour = (index + k + allSessionsList.size()) % allSessionsList.size();

      System.out.println("Last spectated after attacker in session index: " + index);
      System.out.println("Now spectating after attacker in session index: " + neighbour);

      return allSessionsList.get(neighbour);
   }

   /**
    * @return <code>true</code> if there exists attack session that is associated with player with provided id.
    * Otherwise, returns <code>false</code>.
    */
   public boolean hasSessionAssociatedWithPlayerId(int playerId) {
      for (var sessionList : sessions.values()) {
         for (var session : sessionList) {
            if (playerId == session.getAttackingPlayerId()) {
               return true;
            }
         }
      }
      return false;
   }

   /**
    *
    * @return <code>true</code> if there exists an attack session that has <code>playerId</code> as a spectator.
    * Otherwise, returns <code>false</code>.
    */
   public boolean isPlayerInSpectatingMode(int playerId) {
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

   public Optional<AttackSession> getSessionById(int sessionId) {
      for (var sessionList : sessions.values()) {
         for (var session : sessionList) {
            if (sessionId == session.getId()) {
               return Optional.of(session);
            }
         }
      }
      return Optional.empty();
   }

   public Optional<AttackSession> getAnyAttackSessionByTowerId(int towerId) {
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
