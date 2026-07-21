package ec.edu.espe.asignaciones.entities;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "vehicle_assignments")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VehicleAssignment {

    @EmbeddedId
    private AssignmentId id;

    @Column(nullable = false)
    private boolean active;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AssignmentStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "assignment_type", nullable = false, length = 20)
    private AssignmentType assignmentType;

    @Column(name = "authorization_role_id", nullable = false)
    private UUID authorizationRoleId;

    @Column(name = "authorization_role_name", nullable = false, length = 50)
    private String authorizationRoleName;

    @Column(name = "valid_from", nullable = false)
    private OffsetDateTime validFrom;

    @Column(name = "valid_until")
    private OffsetDateTime validUntil;

    @Column(name = "vehicle_alias", length = 80)
    private String vehicleAlias;

    @Column(name = "entry_authorized", nullable = false)
    private boolean entryAuthorized;

    @Column(length = 500)
    private String observation;

    @Column(name = "change_reason", length = 500)
    private String changeReason;

    @Column(name = "assigned_at", nullable = false)
    private OffsetDateTime assignedAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        OffsetDateTime now = OffsetDateTime.now(ZoneId.of("America/Guayaquil"));
        this.assignedAt = now;
        this.updatedAt = now;
        if (this.validFrom == null) {
            this.validFrom = now;
        }
        if (this.status == null) {
            this.status = AssignmentStatus.ACTIVA;
        }
        if (this.assignmentType == null) {
            this.assignmentType = AssignmentType.PROPIETARIO;
        }
        this.entryAuthorized = this.status == AssignmentStatus.ACTIVA;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = OffsetDateTime.now(ZoneId.of("America/Guayaquil"));
    }
}
