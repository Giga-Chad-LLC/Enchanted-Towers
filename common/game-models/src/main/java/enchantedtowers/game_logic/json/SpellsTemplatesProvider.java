package enchantedtowers.game_logic.json;


import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import enchantedtowers.common.utils.proto.common.SpellType;
import enchantedtowers.game_models.utils.Vector2;


public class SpellsTemplatesProvider {
    public static final String FIRE_SPELL_LABEL = "Fire";
    public static final String WIND_SPELL_LABEL = "Wind";
    public static final String EARTH_SPELL_LABEL = "Earth";
    public static final String WATER_SPELL_LABEL = "Water";

    static public class SpellTemplateData {
        private final int id;
        private final List<Vector2> points;
        private final SpellType spellType;

        public SpellTemplateData(int id, List<Vector2> points, SpellType spellType) {
            this.id = id;
            this.points = points;
            this.spellType = spellType;
        }

        public int getId () {
            return id;
        }

        public List<Vector2> getPoints() {
            return points;
        }

        public SpellType getSpellType() {
            return spellType;
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
            SpellType spellType = getSpellTypeByString(template.getString("type"));

            if (!currentPointsArray.isEmpty()) {
                templates.add(new SpellTemplateData(id, currentPointsArray, spellType));
            }
        }

        return templates;
    }

    static private SpellType getSpellTypeByString(String type) {
        if (Objects.equals(type, FIRE_SPELL_LABEL)) {
            return SpellType.FIRE_SPELL;
        }
        else if (Objects.equals(type, WIND_SPELL_LABEL)) {
            return SpellType.WIND_SPELL;
        }
        else if (Objects.equals(type, EARTH_SPELL_LABEL)) {
            return SpellType.EARTH_SPELL;
        }
        else if (Objects.equals(type, WATER_SPELL_LABEL)) {
            return SpellType.WATER_SPELL;
        }
        else {
            return SpellType.UNRECOGNIZED;
        }
    }
}
