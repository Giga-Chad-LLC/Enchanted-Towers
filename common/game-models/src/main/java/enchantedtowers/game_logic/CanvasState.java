package enchantedtowers.game_logic;

import enchantedtowers.game_models.TemplateDescription;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CanvasState {
   private final List<TemplateDescription> spellDescriptions = new ArrayList<>();

   public void addTemplate(TemplateDescription template) {
      spellDescriptions.add(template);
   }

   public List<TemplateDescription> getTemplates() {
      return Collections.unmodifiableList(spellDescriptions);
   }

   public void clear() {
      spellDescriptions.clear();
   }
}
