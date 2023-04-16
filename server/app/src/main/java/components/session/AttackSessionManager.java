package components.session;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

public class AttackSessionManager {
   // towerId -> sessions
   private final Map<Integer, List<AttackSession>> sessions = new TreeMap<>();
   private int CURRENT_SESSION_ID = 0;

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
    * Invariant: if key exist then the list on sessions must be non-empty
   */
   // TODO: replace with getSessionById
   public Optional<AttackSession> getAttackSessionByTowerId(int towerId) {
      if (sessions.containsKey(towerId)) {
         assert(sessions.get(towerId) != null && !sessions.get(towerId).isEmpty());
         // returning first session
         AttackSession firstSession = sessions.get(towerId).get(0);
         return Optional.of(firstSession);
      }

      return Optional.empty();
   }
   // TODO: replace with getSessionById
   public Optional<AttackSession> getAttackSessionByPlayerId(int playerId) {
      for (var sessionList : sessions.values()) {
         for (var session : sessionList) {
            if (playerId == session.getAttackingPlayerId()) {
               return Optional.of(session);
            }
         }
      }

      return Optional.empty();
   }
}
