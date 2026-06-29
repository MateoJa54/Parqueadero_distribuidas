package ec.edu.espe.asignaciones.dtos;

import java.time.OffsetDateTime;
import java.util.UUID;

import ec.edu.espe.asignaciones.entities.AssignmentStatus;
import ec.edu.espe.asignaciones.entities.AssignmentType;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AssignmentResponse {

    private UUID userId;
    private UUID vehicleId;
    private boolean active;
    private AssignmentStatus status;
    private AssignmentType assignmentType;
    private UUID authorizationRoleId;
    private String authorizationRoleName;
    private OffsetDateTime validFrom;
    private OffsetDateTime validUntil;
    private String vehicleAlias;
    private boolean entryAuthorized;
    private String observation;
    private String changeReason;
    private OffsetDateTime assignedAt;
    private OffsetDateTime updatedAt;
}
