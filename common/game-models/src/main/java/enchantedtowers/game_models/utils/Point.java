package enchantedtowers.game_models.utils;

public class Point {
    private double x;
    private double y;
    public Point() {
        x = y = 0.0;
    }
    public Point(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public double getX() {
        return x;
    }

    public void setX(double x) {
        this.x = x;
    }

    public double getY() {
        return y;
    }

    public void setY(double y) {
        this.y = y;
    }
}
