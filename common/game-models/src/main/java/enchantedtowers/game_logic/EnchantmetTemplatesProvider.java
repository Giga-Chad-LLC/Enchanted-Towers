package enchantedtowers.game_logic;


import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import enchantedtowers.game_models.Spell;
import enchantedtowers.game_models.SpellTemplate;
import enchantedtowers.game_models.utils.Vector2;


public class EnchantmetTemplatesProvider {
    static public List<SpellTemplate> parseJson(String jsonString) throws JSONException {
        JSONObject content = new JSONObject(jsonString);
        JSONArray templatesJson = content.getJSONArray("canvasTemplates");

        List<SpellTemplate> templates = new ArrayList<>();

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
                templates.add(new SpellTemplate(id, currentPointsArray));
            }
        }

        return templates;
    }
}
