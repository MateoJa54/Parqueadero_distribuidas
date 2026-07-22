package ec.edu.espe.asignaciones.dtos;

import java.time.OffsetDateTime;
import java.util.UUID;

import ec.edu.espe.asignaciones.entities.AssignmentStatus;
import ec.edu.espe.asignaciones.entities.AssignmentType;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class FleetVehicleResponse {

    private UUID userId;
    private UUID vehicleId;
    private String placa;
    private String marca;
    private String modelo;
    private String color;
    private Integer anio;
    private String tipo;
    private String clasificacion;
    private boolean activo;
    private AssignmentStatus status;
    private AssignmentType assignmentType;
    private String authorizationRoleName;
    private OffsetDateTime validFrom;
    private OffsetDateTime validUntil;
    private String vehicleAlias;
    private boolean entryAuthorized;
    private OffsetDateTime assignedAt;
}
