package enchantedtowers.game_logic;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CanvasState {
   private final List<MatchedTemplateDescription> spellDescriptions = new ArrayList<>();

   public void addTemplate(MatchedTemplateDescription template) {
      spellDescriptions.add(template);
   }

   public List<MatchedTemplateDescription> getTemplates() {
      return Collections.unmodifiableList(spellDescriptions);
   }

   public void clear() {
      spellDescriptions.clear();
   }
}
