package components.db.models.game;

import jakarta.persistence.*;


@Entity
@Table(name = "protection_walls")
public class ProtectionWallModel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "tower_id")
    private TowerModel tower;

    @ManyToOne
    @JoinColumn(name = "state_id")
    private ProtectionWallStateModel wallState;

    public ProtectionWallModel() {}

    public ProtectionWallModel(TowerModel tower, ProtectionWallStateModel wallState) {
        this.tower = tower;
        this.wallState = wallState;
    }

    @Override
    public String toString() {
        return "ProtectionWallModel[id=" + id + ", towerId=" + tower.getId() + ", wallStateId=" + wallState.getId() + "]";
    }
}
