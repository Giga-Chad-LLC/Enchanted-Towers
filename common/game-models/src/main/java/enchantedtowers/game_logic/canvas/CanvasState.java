package enchantedtowers.game_logic.canvas;

import enchantedtowers.game_models.SpellTemplateDescription;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CanvasState {
   private final List<SpellTemplateDescription> spellDescriptions = new ArrayList<>();

   public void addTemplate(SpellTemplateDescription template) {
      spellDescriptions.add(template);
   }

   public List<SpellTemplateDescription> getTemplates() {
      return Collections.unmodifiableList(spellDescriptions);
   }

   public void clear() {
      spellDescriptions.clear();
   }
}
