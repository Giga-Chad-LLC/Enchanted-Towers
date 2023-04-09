package enchantedtowers.game_logic;

import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;

import java.util.Map;
import java.util.Optional;

import enchantedtowers.game_models.Spell;
import enchantedtowers.game_models.utils.Vector2;

public class SpellsPatternMatchingAlgorithm {
    private static final float SIMILARITY_THRESHOLD = 0.80f;

    public record MatchedTemplateDescription(int id, int colorId, Vector2 offset) {}

    public static <Metric extends CurvesMatchingMetric>
    Optional<MatchedTemplateDescription> getMatchedTemplate(Map<Integer, Spell> templates, Spell pattern, int patternColor, Metric metric) {
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

            float similarity = metric.calculate(template.getCurve(), scaledPatternCurve);
            System.out.println("Template " + templateId + " similarity: " + similarity);

            if (maxSimilarity < similarity) {
                maxSimilarity = similarity;
                matchedTemplateId = templateId;
            }
        }

        if (maxSimilarity < SIMILARITY_THRESHOLD) {
            System.out.println("Matched template: none");
            return Optional.empty();
        }

        System.out.println("Matched template: " + matchedTemplateId);

        Vector2 patternOffset = pattern.getOffset();
        Vector2 matchedTemplateOffset = new Vector2(
                patternOffset.x + (patternBounds.getWidth() - templateBounds.getWidth()) / 2,
                patternOffset.y + (patternBounds.getHeight() - templateBounds.getHeight()) / 2
        );

//        Spell matchedTemplate = new Spell(templates.get(matchedTemplateId));
//        matchedTemplate.setOffset(
//                new Vector2(
//                        patternOffset.x + (patternBounds.getWidth() - templateBounds.getWidth()) / 2,
//                        patternOffset.y + (patternBounds.getHeight() - templateBounds.getHeight()) / 2
//                )
//        );

        return Optional.of(new MatchedTemplateDescription(matchedTemplateId, patternColor, matchedTemplateOffset));
    }
}
