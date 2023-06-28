package components.db.models.game;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.Optional;

@Entity
@Table(name = "towers")
public class TowerModel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @OneToOne
    @JoinColumn(name = "position_id")
    private PositionModel positionModel;

    @Column(name = "owner_id")
    private Optional<Integer> ownerId;

    @Column(name = "owner_username")
    private Optional<String> ownerUsername;

    @Column(name = "last_protection_wall_modification_timestamp")
    private Optional<Instant> lastProtectionWallModificationTimestamp;

    @Column(name = "is_under_protection_walls_installation")
    private Boolean isUnderProtectionWallsInstallation;

    @Column(name = "is_under_capture_lock")
    private Boolean isUnderCaptureLock;

    @Column(name = "is_under_attack")
    private Boolean isUnderAttack;

    TowerModel() {}
}
