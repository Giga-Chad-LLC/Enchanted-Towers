package enchantedtowers.common.utils.fs;


import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import enchantedtowers.game_models;


public class EnchantmetTemplatesFileReader {
    static public List<List<Vector2>> parseJson(String jsonString) throws JSONException {
        JSONObject content = new JSONObject(jsonString);
        JSONArray templates = content.getJSONArray("canvasTemplates");

        List<List<Vector2>> templatePoints = new ArrayList<>();

        for (int i = 0; i < templates.length(); i++) {
            JSONObject template = templates.getJSONObject(i);

            List<Vector2> currentPointsArray = new ArrayList<>();

            if (!template.isNull("points")) {
                JSONArray points = template.getJSONArray("points");

                for (int j = 0; j < points.length(); ++j) {
                    JSONArray pointArray = points.getJSONArray(j);

                    double x = pointArray.getDouble(0);
                    double y = pointArray.getDouble(1);

                    currentPointsArray.add(new Vector2(x, y));
                }
            }

            if (!currentPointsArray.isEmpty()) {
                templatePoints.add(currentPointsArray);
            }
        }

        return templatePoints;
    }
}
