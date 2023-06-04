package enchantedtowers.game_logic.json;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import enchantedtowers.game_models.utils.Vector2;

public class DefendSpellsTemplatesProvider {
    static public class DefendSpellTemplateData {
        private final int id;
        private final List<List<Vector2>> lines;

        public DefendSpellTemplateData( int id, List<List<Vector2>> lines) {
            this.id = id;
            this.lines = lines;
        }

        public int getId () {
            return id;
        }

        public List<List<Vector2>> getPoints() {
            return lines;
        }
    }

    static public List<DefendSpellTemplateData> parseDefendSpellsJson(String jsonString) throws JSONException {
        JSONObject content = new JSONObject(jsonString);
        JSONArray defendSpellsJson = content.getJSONArray("defendSpellsTemplates");

        List<DefendSpellTemplateData> defendSpellsData = new ArrayList<>();

        for (int i = 0; i < defendSpellsJson.length(); i++) {
            JSONObject defendSpell = defendSpellsJson.getJSONObject(i);

            List<List<Vector2>> currentLinesArray = new ArrayList<>();

            if (!defendSpell.isNull("points")) {
                JSONArray lines = defendSpell.getJSONArray("points");

                for (int j = 0; j < lines.length(); ++j) {
                    JSONArray pointArray = lines.getJSONArray(j);
                    List<Vector2> currentPointsArray = new ArrayList<>();

                    for (int k = 0; k < pointArray.length(); ++k) {
                        double x = pointArray.getDouble(0);
                        double y = pointArray.getDouble(1);

                        currentPointsArray.add(new Vector2(x, y));
                    }

                    currentLinesArray.add(currentPointsArray);
                }
            }

            int id = defendSpell.getInt("id");

            if (!currentLinesArray.isEmpty()) {
                defendSpellsData.add(new DefendSpellTemplateData(id, currentLinesArray));
            }
        }

        return defendSpellsData;
    }
}
