package enchantedtowers.game_logic.algorithm;

import enchantedtowers.common.utils.proto.common.SpellType;
import enchantedtowers.game_models.SpellBook;
import enchantedtowers.game_models.SpellTemplateDescription;
import enchantedtowers.game_models.utils.Utils;
import java.util.List;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;

import java.util.Map;
import java.util.Optional;

import enchantedtowers.game_models.Spell;
import enchantedtowers.game_models.utils.Vector2;

public class SpellsMatchingAlgorithm {
    private static final float SPELL_SIMILARITY_THRESHOLD = 0.80f;

    static public Optional<SpellTemplateDescription> getMatchedTemplateWithHausdorffMetric(
        List<Vector2> spellPoints, Vector2 offset, SpellType spellType) {
        if (Utils.isValidPath(spellPoints)) {
            Spell pattern = new Spell(
                Utils.getNormalizedPoints(spellPoints, offset),
                offset
            );

            return getMatchedTemplate(
                SpellBook.getSpellTemplates(),
                pattern,
                spellType,
                new HausdorffMetric()
            );
        }

        return Optional.empty();
    }

    static private <Metric extends CurvesMatchingMetric>
    Optional<SpellTemplateDescription> getMatchedTemplate(Map<Integer, Spell> templates,
                                                          Spell pattern, SpellType patternSpellType, Metric metric) {
        Envelope patternBounds = pattern.getBoundary();

        Envelope templateBounds = new Envelope();
        float maxSimilarity = 0f;
        Integer matchedTemplateId = null;

        for (var entry : templates.entrySet()) {
            final Spell template = entry.getValue();
            final int templateId = entry.getKey();

            templateBounds = template.getBoundary();


            // scale is required ONLY for computing the metric,
            // thus, should not be considered in any other pattern-transformations
            Geometry scaledPatternCurve = pattern.getScaledCurve(
                    templateBounds.getWidth() / patternBounds.getWidth(),
                    templateBounds.getHeight() / patternBounds.getHeight(),
                    0,
                    0
            );

            float similarity = metric.calculate(template.getCurveCopy(), scaledPatternCurve);
            System.out.println("Template " + templateId + " similarity: " + similarity);

            if (maxSimilarity < similarity) {
                maxSimilarity = similarity;
                matchedTemplateId = templateId;
            }
        }

        if (maxSimilarity < SPELL_SIMILARITY_THRESHOLD) {
            System.out.println("Matched template: none");
            return Optional.empty();
        }

        System.out.println("Matched template: " + matchedTemplateId);

        Vector2 patternOffset = pattern.getOffset();
        // recalculate matched template boundaries
        templateBounds = templates.get(matchedTemplateId).getBoundary();

        Vector2 matchedTemplateOffset = new Vector2(
                patternOffset.x + (patternBounds.getWidth() - templateBounds.getWidth()) / 2,
                patternOffset.y + (patternBounds.getHeight() - templateBounds.getHeight()) / 2
        );

        return Optional.of(new SpellTemplateDescription(matchedTemplateId, patternSpellType, matchedTemplateOffset));
    }
}
