package enchantedtowers.game_models.utils;

public class Vector2 {
    public double x;
    public double y;

    public Vector2(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public Vector2(Vector2 that) {
        x = that.x;
        y = that.y;
    }

    public void move(double dx, double dy) {
        x += dx;
        y += dy;
    }

    @Override
    public String toString() {
        return "Vector2[" + x + ", " + y + "]";
    }
}
