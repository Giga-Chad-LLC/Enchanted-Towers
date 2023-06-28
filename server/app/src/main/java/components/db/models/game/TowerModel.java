package components.db.models.game;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "towers")
public class TowerModel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @OneToOne
    @JoinColumn(name = "position_id")
    private PositionModel positionModel;

    // TODO: mark nullable?
    @Column(name = "owner_id")
    private Integer ownerId;

    @Column(name = "owner_username")
    private String ownerUsername;

    @Column(name = "last_protection_wall_modification_timestamp")
    private Instant lastProtectionWallModificationTimestamp;

    @Column(name = "is_under_protection_walls_installation")
    private Boolean isUnderProtectionWallsInstallation;

    @Column(name = "is_under_capture_lock")
    private Boolean isUnderCaptureLock;

    @Column(name = "is_under_attack")
    private Boolean isUnderAttack;

    TowerModel() {}

    public Integer getId() {
        return id;
    }
}
