package components.db.models.game;

import jakarta.persistence.*;


@Entity
@Table(name = "positions")
public class PositionModel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @OneToOne
    @JoinColumn(name = "tower_id")
    private TowerModel tower;

    @Column(name = "x")
    private Double x;

    @Column(name = "y")
    private Double y;

    public PositionModel() {}

    public PositionModel(double x, double y) {
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

    @Override
    public String toString() {
        return "PositionModel[id=" + id + ", towerId=" + tower.getId() + ", x=" + x + ", y=" + y + "]";
    }
}
