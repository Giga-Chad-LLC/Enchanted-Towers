package enchantedtowers.game_logic;

import enchantedtowers.game_models.utils.Vector2;
import java.util.ArrayList;
import java.util.List;

public class SpellDrawingDescription {
   private List<Vector2> points = new ArrayList<>();
   private int colorId = 0;
   private Vector2 offset = null;

   public void reset() {
      points.clear();
      colorId = 0;
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

   public void setColorId(int colorId) {
      this.colorId = colorId;
   }

   public int getColorId() {
      return colorId;
   }

   public void setOffset(Vector2 offset) {
      this.offset = offset;
   }

   public Vector2 getOffset() {
      return offset;
   }
}
