package enchantedtowers.game_logic.algorithm;

import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;

import java.util.List;
import java.util.Map;
import java.util.Objects;
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

    static public double getTemplateMatchPercentageWithHausdorffMetric(
            int defendSpellId,
            List<List<Vector2>> defendSpellLines
    ) {
        List<List<Vector2>> normalizedLines = Utils.getNormalizedLines(defendSpellLines);
        DefendSpell pattern = new DefendSpell(normalizedLines);

        logger.info(SpellBook.getDefendSpellsTemplates().toString());
        return getMatchPercentageWithTemplate(
                defendSpellId,
                SpellBook.getDefendSpellTemplateById(defendSpellId),
                pattern,
                new HausdorffMetric()
        );
    }

    static public boolean isMatchedWithTemplate(double similarity) {
        return similarity >= DEFEND_SPELL_SIMILARITY_THRESHOLD;
    }

    static private <Metric extends CurvesMatchingMetric>
    double getMatchPercentageWithTemplate(
            int templateId,
            DefendSpell template,
            DefendSpell pattern,
            Metric metric
    ) {
        if (template == null) {
            System.out.println("Provided defend spell template is null");
            return 0.0;
        }

        Envelope patternBounds = pattern.getBoundary();
        Envelope templateBounds = new Envelope();

        templateBounds = template.getBoundary();
        Geometry scaledPatternCurveUnion = pattern.getScaledCurve(
                templateBounds.getWidth() / patternBounds.getWidth(),
                templateBounds.getHeight() / patternBounds.getHeight(),
                0,
                0
        );

        float similarity = metric.calculate(template.getCurveUnionCopy(), scaledPatternCurveUnion);
        logger.info("Defend spell template " + templateId + " similarity: " + similarity);

        return similarity;
    }
}
