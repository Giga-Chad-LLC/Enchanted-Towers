package enchantedtowers.game_logic;

import enchantedtowers.game_models.utils.Vector2;

public class MatchedTemplateDescription {
   private final int id;
   private final int colorId;
   private final Vector2 offset;

   MatchedTemplateDescription(int id, int colorId, Vector2 offset) {
      this.id = id;
      this.colorId = colorId;
      this.offset = offset;
   }

   public int id() {
      return id;
   }

   public int colorId() {
      return colorId;
   }

   public Vector2 offset() {
      return offset;
   }
}
