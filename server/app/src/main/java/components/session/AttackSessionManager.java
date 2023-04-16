package components.session;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

public class AttackSessionManager {
   // towerId -> sessions
   private final Map<Integer, List<AttackSession>> sessions = new TreeMap<>();
   private int CURRENT_SESSION_ID = 2;

   /**
    * Returns id of newly created session
    */
   public int add(int playerId, int towerId) {
      if (!sessions.containsKey(towerId)) {
         sessions.put(towerId, new ArrayList<>());
      }

      int sessionId = CURRENT_SESSION_ID++;

      AttackSession session = new AttackSession(sessionId, playerId, towerId);
      sessions.get(towerId).add(session);

      return sessionId;
   }

   public void remove(AttackSession session) {
      for (var sessionList : sessions.values()) {
         if (sessionList.contains(session)) {
            sessionList.remove(session);
            return;
         }
      }
   }

   /**
    * @param playerId
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
    * @param playerId
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

   /**
    * Invariant: if key exists then the list of sessions must be non-empty
    */
   public Optional<AttackSession> getAnyAttackSessionByTowerId(int towerId) {
      if (sessions.containsKey(towerId)) {
         assert(sessions.get(towerId) != null && !sessions.get(towerId).isEmpty());
         // returning first session
         AttackSession firstSession = sessions.get(towerId).get(0);
         return Optional.of(firstSession);
      }

      return Optional.empty();
   }
}
