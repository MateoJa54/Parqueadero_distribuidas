package ec.edu.espe.asignaciones.dtos;

import java.time.OffsetDateTime;
import java.util.UUID;

import ec.edu.espe.asignaciones.entities.AuditAction;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AuditEventResponse {

    private UUID id;
    private UUID userId;
    private UUID vehicleId;
    private AuditAction action;
    private OffsetDateTime timestamp;
    private String oldPayload;
    private String newPayload;
}
