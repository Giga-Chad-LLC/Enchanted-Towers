package enchantedtowers.game_logic.canvas;

import enchantedtowers.common.utils.proto.common.SpellType;
import enchantedtowers.game_models.utils.Vector2;
import java.util.ArrayList;
import java.util.List;

public class SpellDrawingDescription {
   private List<Vector2> points = new ArrayList<>();
   private SpellType spellType = SpellType.UNRECOGNIZED;
   private Vector2 offset = null;

   public void reset() {
      points.clear();
      spellType = SpellType.UNRECOGNIZED;
      offset = null;
   }

   public List<Vector2> getPoints() {
      return points;
   }
   
   public void setPoints(List<Vector2> points) {
      this.points = points;
   }
   
   public void addPoint(Vector2 point) {
      points.add(point);
   }

   public void setSpellType(SpellType spellType) {
      this.spellType = spellType;
   }

   public SpellType getSpellType() {
      return spellType;
   }

   public void setOffset(Vector2 offset) {
      this.offset = offset;
   }

   public Vector2 getOffset() {
      return offset;
   }
}
