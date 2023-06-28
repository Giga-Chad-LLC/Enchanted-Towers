package components.defend_spells;

import components.time.Timeout;

public class DefendSpellTimeout {
   private static final int DEFEND_SPELL_EXPIRATION_TIMEOUT_MS = 30 * 1000;

   private final long creationTimestampMs = System.currentTimeMillis();
   private final Timeout timeout;
   private final int id;

   public DefendSpellTimeout(int defendSpellId, Runnable callback) {
      this.id = defendSpellId;
      this.timeout = new Timeout(DEFEND_SPELL_EXPIRATION_TIMEOUT_MS, callback);
   }

   public int getId() {
      return id;
   }

   public long getExpirationTimeoutMs() {
      return DEFEND_SPELL_EXPIRATION_TIMEOUT_MS;
   }

   public long getLeftExecutionTimeMs() {
      long pastTime_ms = System.currentTimeMillis() - creationTimestampMs;
      return Math.max(getExpirationTimeoutMs() - pastTime_ms, 0);
   }
}
