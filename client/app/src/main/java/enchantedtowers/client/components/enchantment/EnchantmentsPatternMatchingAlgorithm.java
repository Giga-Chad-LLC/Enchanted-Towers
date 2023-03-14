package enchantedtowers.client.components.enchantment;

import android.graphics.PointF;
import android.graphics.RectF;

import org.locationtech.jts.algorithm.distance.DiscreteFrechetDistance;

import java.util.ArrayList;
import java.util.List;

public class EnchantmentsPatternMatchingAlgorithm {
    private static float getNormalizedMatchCoefficientByTemplateBounds(RectF bounds, float cost) {
        float tw = bounds.width();
        float th = bounds.height();
        float maxCostNumber = (float) Math.sqrt(tw * tw + th * th);
        return cost / maxCostNumber;
    }

    private static float MATCH_COEFFICIENT_THRESHOLD = 0.2f;

    public static <Metric extends CurvesMatchingMetric>
    Enchantment getMatchedTemplate(List<Enchantment> templates, Enchantment pattern, Metric metric) {
        Enchantment patternCopy = new Enchantment(pattern);
        RectF patternBounds = new RectF();
        patternCopy.setOffset(new PointF(0f, 0f));
        patternCopy.getPath().computeBounds(patternBounds, true);

        RectF templateBounds = new RectF();
        float minCost = Float.POSITIVE_INFINITY;
        float minMatchCoefficient = Float.POSITIVE_INFINITY;
        int matchedTemplateIndex = 0;

        for (int i = 0; i < templates.size(); i++) {
            Enchantment template = templates.get(i);
            template.getPath().computeBounds(templateBounds, true);

            // scale is required ONLY for computing the metric,
            // thus, should not be considered in any other pattern-transformations
            float[] scaledPoints = patternCopy.getScaledPoints(
                    templateBounds.width() / patternBounds.width(),
                    templateBounds.height() / patternBounds.height(),
                    0,
                    0
            );

            float cost = metric.calculate(template.getPoints(), scaledPoints);
            System.out.println("Template " + i + ", cost " + cost);

            // threshold: at least one template <= 0.2 is good, otherwise we say no template found and delete the enchantment
            float matchCoefficient = getNormalizedMatchCoefficientByTemplateBounds(templateBounds, cost);
            System.out.println("Normalized match coefficient: " + matchCoefficient);
            minMatchCoefficient = Math.min(minMatchCoefficient, matchCoefficient);

            if (minCost > cost) {
                minCost = cost;
                matchedTemplateIndex = i;
            }
        }

        if (minMatchCoefficient > MATCH_COEFFICIENT_THRESHOLD) {
            System.out.println("Matched template: none");
            return null;
        }

        System.out.println("Matched template: " + matchedTemplateIndex);
        Enchantment matchedTemplate = new Enchantment(templates.get(matchedTemplateIndex));

        PointF patternOffset = pattern.getOffset();
        matchedTemplate.setOffset(
                new PointF(
                        patternOffset.x + (patternBounds.width() - templateBounds.width()) / 2,
                        patternOffset.y + (patternBounds.height() - templateBounds.height()) / 2
                )
        );

        return matchedTemplate;
    }
}
