package enchantedtowers.client.components.enchantment;

import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;

import java.util.List;

import enchantedtowers.game_models.utils.Point;

public class SpellsPatternMatchingAlgorithm {
    private static final float SIMILARITY_THRESHOLD = 0.80f;

    public static <Metric extends CurvesMatchingMetric>
    Spell getMatchedTemplate(List<Spell> templates, Spell pattern, Metric metric) {
        Envelope patternBounds = pattern.getBoundary();

        Envelope templateBounds = new Envelope();
        float maxSimilarity = 0f;
        int matchedTemplateIndex = 0;

        for (int i = 0; i < templates.size(); i++) {
            Spell template = templates.get(i);
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
            System.out.println("Template " + i + " similarity: " + similarity);

            if (maxSimilarity < similarity) {
                maxSimilarity = similarity;
                matchedTemplateIndex = i;
            }
        }

        if (maxSimilarity < SIMILARITY_THRESHOLD) {
            System.out.println("Matched template: none");
            return null;
        }

        System.out.println("Matched template: " + matchedTemplateIndex);
        Spell matchedTemplate = new Spell(templates.get(matchedTemplateIndex));

        Point patternOffset = pattern.getOffset();
        matchedTemplate.setOffset(
                new Point(
                        patternOffset.x + (patternBounds.getWidth() - templateBounds.getWidth()) / 2,
                        patternOffset.y + (patternBounds.getHeight() - templateBounds.getHeight()) / 2
                )
        );

        return matchedTemplate;
    }
}
