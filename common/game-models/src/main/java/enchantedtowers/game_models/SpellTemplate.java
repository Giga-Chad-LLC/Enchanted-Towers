package enchantedtowers.game_models;

import java.util.List;

import enchantedtowers.game_models.utils.Vector2;

public class SpellTemplate {
    private final int id;
    private final List<Vector2> points;

    public SpellTemplate(int id, List<Vector2> points) {
        this.id = id;
        this.points = points;
    }

    public int getId() {
        return id;
    }

    public List<Vector2> getPoints() {
        return points;
    }
};