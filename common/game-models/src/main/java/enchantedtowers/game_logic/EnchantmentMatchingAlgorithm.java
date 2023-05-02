package enchantedtowers.game_logic;

import enchantedtowers.common.utils.proto.common.SpellType;
import enchantedtowers.game_models.Enchantment;
import enchantedtowers.game_models.Spell;
import enchantedtowers.game_models.SpellBook;
import enchantedtowers.game_models.utils.Vector2;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.util.AffineTransformation;

// TODO: get the percentage of match for each color separately
public class EnchantmentMatchingAlgorithm {
   private final static Logger logger = Logger.getLogger(EnchantmentMatchingAlgorithm.class.getName());

   static public Map<SpellType, Double> getEnchantmentMatchStatsWithHausdorffMetric(Enchantment guess, Enchantment actual) {
      CurvesMatchingMetric metric = new HausdorffMetric();
      Map<SpellType, Double> results = new HashMap<>();

      for (var type : SpellBook.getAllSpellTypes()) {
         results.put(
             type,
             getPercentageMatchBySpellType(guess, actual, type, metric)
         );
      }

      return results;
   }


   static private <Metric extends CurvesMatchingMetric>
   double getPercentageMatchBySpellType(Enchantment guess, Enchantment actual, SpellType spellType, Metric metric) {
      Geometry guessGeometry = getEnchantmentGeometryBySpellType(guess, spellType);
      Geometry actualGeometry = getEnchantmentGeometryBySpellType(actual, spellType);

      return metric.calculate(guessGeometry, actualGeometry);
   }

   /**
    * @return Combined geometry of all curves in {@code enchantment} with the spell type of {@code spellType}.
    */
   static private Geometry getEnchantmentGeometryBySpellType(Enchantment enchantment, SpellType spellType) {
      GeometryFactory factory = new GeometryFactory();
      List<Geometry> curves = new ArrayList<>();

      for (var template : enchantment.getTemplateDescriptions()) {
         if (template.spellType() != spellType) {
            continue;
         }

         Spell spell = SpellBook.getTemplateById(template.id());
         if (spell == null) {
            logger.warning("In getEnchantmentGeometryBySpellType(): spell template not found, template id " + template.id());
            continue;
         }

         // apply translation to the curve and add it to the list
         Geometry curve = spell.getCurveCopy();
         Vector2 offset = spell.getOffset();
         curve.apply(
             AffineTransformation.translationInstance(offset.x, offset.y)
         );
         curves.add(curve);
      }

      logger.info("Curves list for spell type: " + spellType);
      for (var geometry : curves) {
         logger.info(geometry.toText());
      }

      Geometry[] geometries = new Geometry[curves.size()];
      curves.toArray(geometries);
      GeometryCollection geometryCollection = new GeometryCollection(geometries, factory);
      Geometry combinedGeometry = geometryCollection.union();

      logger.info("Combined geometry for type: " + spellType + ", geometry: " + combinedGeometry.toText());

      return combinedGeometry;
   }
}
