package components.db.models.game;

import jakarta.persistence.*;


@Entity
@Table(name = "positions")
public class PositionModel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(name = "x")
    private final double x;

    @Column(name = "y")
    private final double y;

    PositionModel() {
        x = 0;
        y = 0;
    }

    PositionModel(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public Integer getId() {
        return id;
    }

    public Double getX() {
        return x;
    }

    public Double getY() {
        return y;
    }
}
