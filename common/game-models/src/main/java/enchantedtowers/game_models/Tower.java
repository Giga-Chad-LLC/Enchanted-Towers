package enchantedtowers.game_models;

public class Tower {
    private final double x;
    private final double y;

    Tower(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public double getY() {
        return y;
    }

    public double getX() {
        return x;
    }
}
