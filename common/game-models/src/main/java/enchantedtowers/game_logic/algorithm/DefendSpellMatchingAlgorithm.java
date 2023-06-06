package enchantedtowers.game_logic.algorithm;

import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;

import enchantedtowers.game_models.DefendSpell;
import enchantedtowers.game_models.DefendSpellTemplateDescription;
import enchantedtowers.game_models.SpellBook;
import enchantedtowers.game_models.utils.Utils;
import enchantedtowers.game_models.utils.Vector2;

public class DefendSpellMatchingAlgorithm {
    private static final float DEFEND_SPELL_SIMILARITY_THRESHOLD = 0.70f;
    private final static Logger logger = Logger.getLogger(DefendSpellMatchingAlgorithm.class.getName());

    static public Optional<DefendSpellTemplateDescription> getMatchedTemplateWithHausdorffMetric(
            List<List<Vector2>> defendSpellLines
    ) {
        List<List<Vector2>> normalizedLines = Utils.getNormalizedLines(defendSpellLines);
        DefendSpell pattern = new DefendSpell(normalizedLines);

        logger.info(SpellBook.getDefendSpellsTemplates().toString());
        return getMatchedTemplate(
                SpellBook.getDefendSpellsTemplates(),
                pattern,
                new HausdorffMetric()
        );
    }

    static private <Metric extends CurvesMatchingMetric>
    Optional<DefendSpellTemplateDescription> getMatchedTemplate(Map<Integer, DefendSpell> templates,
                                                          DefendSpell pattern, Metric metric) {
        Envelope patternBounds = pattern.getBoundary();
        Envelope templateBounds = new Envelope();

        float maxSimilarity = 0f;
        Integer matchedTemplateId = null;

        for (var entry : templates.entrySet()) {
            final DefendSpell template = entry.getValue();
            final int templateId = entry.getKey();

            templateBounds = template.getBoundary();
            Geometry scaledPatternCurveUnion = pattern.getScaledCurve(
                    templateBounds.getWidth() / patternBounds.getWidth(),
                    templateBounds.getHeight() / patternBounds.getHeight(),
                    0,
                    0
            );

            float similarity = metric.calculate(template.getCurveUnionCopy(), scaledPatternCurveUnion);
            logger.info("Defend spell template " + templateId + " similarity: " + similarity);

            if (maxSimilarity < similarity) {
                maxSimilarity = similarity;
                matchedTemplateId = templateId;
            }
        }

        if (maxSimilarity < DEFEND_SPELL_SIMILARITY_THRESHOLD) {
            logger.info("Matched defend spell template: none");
            return Optional.empty();
        }

        logger.info("Matched defend spell template: " + matchedTemplateId);
        return Optional.of(new DefendSpellTemplateDescription(matchedTemplateId));
    }
}
