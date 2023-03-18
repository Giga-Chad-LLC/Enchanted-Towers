package enchantedtowers.client.components.fs;

import android.content.Context;
import android.graphics.PointF;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class EnchantmetTemplatesFileReader extends JSONFileReader {

    public EnchantmetTemplatesFileReader(Context context) {
        super(context);
    }

    public List<List<PointF>> processFile(int resourceId) throws IOException, JSONException {
        String json = readRawFile(resourceId);
        return parseJsonFromString(json);
    }

    private List<List<PointF>> parseJsonFromString(String jsonString) throws JSONException {
        JSONObject content = new JSONObject(jsonString);
        JSONArray templates = content.getJSONArray("canvasTemplates");

        List <List<PointF>> templatePoints = new ArrayList<>();

        for (int i = 0; i < templates.length(); i++) {
            JSONObject template = templates.getJSONObject(i);

            List<PointF> currentPointsArray = new ArrayList<>();

            if (!template.isNull("points")) {
                JSONArray points = template.getJSONArray("points");

                for (int j = 0; j < points.length(); ++j) {
                    JSONArray pointArray = points.getJSONArray(j);

                    float x = (float)pointArray.getDouble(0);
                    float y = (float)pointArray.getDouble(1);

                    currentPointsArray.add(new PointF(x, y));
                }
            }

            if (!currentPointsArray.isEmpty()) {
                templatePoints.add(currentPointsArray);
            }
        }

        return templatePoints;
    }
}
