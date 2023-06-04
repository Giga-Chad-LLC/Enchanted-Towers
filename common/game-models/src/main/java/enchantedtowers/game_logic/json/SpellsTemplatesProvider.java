package enchantedtowers.game_logic.json;


import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import enchantedtowers.game_models.utils.Vector2;


public class SpellsTemplatesProvider {
    static public class SpellTemplateData {
        private final int id;
        private final List<Vector2> points;

        public SpellTemplateData( int id, List<Vector2 > points) {
            this.id = id;
            this.points = points;
        }

        public int getId () {
            return id;
        }

        public List<Vector2> getPoints () {
            return points;
        }
    }

    static public List<SpellTemplateData> parseSpellsJson(String jsonString) throws JSONException {
        JSONObject content = new JSONObject(jsonString);
        JSONArray templatesJson = content.getJSONArray("spellsTemplates");

        List<SpellTemplateData> templates = new ArrayList<>();

        for (int i = 0; i < templatesJson.length(); i++) {
            JSONObject template = templatesJson.getJSONObject(i);

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

            int id = template.getInt("id");

            if (!currentPointsArray.isEmpty()) {
                templates.add(new SpellTemplateData(id, currentPointsArray));
            }
        }

        return templates;
    }
}
