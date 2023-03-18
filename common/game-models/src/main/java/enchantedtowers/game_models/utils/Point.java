package enchantedtowers.game_models.utils;

public class Point {
    public double x;
    public double y;

    public Point(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public Point(Point that) {
        x = that.x;
        y = that.y;
    }

    public void move(double dx, double dy) {
        x += dx;
        y += dy;
    }
}
