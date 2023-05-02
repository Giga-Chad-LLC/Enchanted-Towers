package enchantedtowers.game_models;

import enchantedtowers.game_models.utils.Vector2;
import enchantedtowers.common.utils.proto.common.SpellType;

public class TemplateDescription {
   private final int id;
   SpellType spellType;
   private final Vector2 offset;

   public TemplateDescription(int id, SpellType spellType, Vector2 offset) {
      this.id = id;
      this.spellType = spellType;
      this.offset = offset;
   }

   public int id() {
      return id;
   }

   public SpellType spellType() {
      return spellType;
   }

   public Vector2 offset() {
      return offset;
   }
}
