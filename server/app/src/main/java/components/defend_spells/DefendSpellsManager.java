package components.defend_spells;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DefendSpellsManager {
   // towerId -> defend spells timers
   private final Map<Integer, List<DefendSpellTimeout>> defendSpellTimers = new ConcurrentHashMap<>();

   public void addDefendSpell(int towerId, int defendSpellId, Runnable callback) {
      if (!defendSpellTimers.containsKey(towerId)) {
         defendSpellTimers.put(
             towerId, new ArrayList<>()
         );
      }

      defendSpellTimers.get(towerId).add(new DefendSpellTimeout(
          defendSpellId,
          callback
      ));
   }

   public void removeDefendSpell(int towerId, int defendSpellId) {
      if (!defendSpellTimers.containsKey(towerId)) {
         return;
      }
      defendSpellTimers.get(towerId).removeIf((defendTimeout) -> defendTimeout.getId() == defendSpellId);
   }

   public List<DefendSpellTimeout> getActiveDefendSpellsByTowerId(int towerId) {
      if (!defendSpellTimers.containsKey(towerId)) {
         return List.of();
      }

      return defendSpellTimers.get(towerId).stream().filter(defendTimeout -> defendTimeout.getExpirationTimeoutMs() != 0).toList();
   }

   public boolean isDefendSpellAlreadyCasted(int towerId, int defendSpellId) {
      if (!defendSpellTimers.containsKey(towerId)) {
         return false;
      }

      List<DefendSpellTimeout> timeouts = defendSpellTimers.get(towerId);

      for (var timeout : timeouts) {
         if (timeout.getId() == defendSpellId) {
            return true;
//            throw new Exception("Defend spell '" + Objects.requireNonNull(
//                SpellBook.getDefendSpellTemplateById(defendSpellId)).getName() + "' already casted");
         }
      }

      return false;
   }
}
