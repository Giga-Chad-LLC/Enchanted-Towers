package components.db.models.game;

import jakarta.persistence.*;


@Entity
@Table(name = "protection_wall_states")
public class ProtectionWallStateModel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "broken")
    private Boolean broken = false;

    @Column(name = "enchanted")
    private Boolean enchanted = false;

    @OneToOne
    @JoinColumn(name = "protection_wall_id")
    private ProtectionWallModel protectionWall;

    public ProtectionWallStateModel() {}

    public Integer getId() {
        return id;
    }

    @Override
    public String toString() {
        return "ProtectionWallStateModel[id=" + id + ", broken=" + broken + ", enchanted=" + enchanted + "]";
    }
}
