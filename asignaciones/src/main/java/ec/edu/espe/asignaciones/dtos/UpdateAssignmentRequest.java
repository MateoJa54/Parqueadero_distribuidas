package ec.edu.espe.asignaciones.dtos;

import java.time.OffsetDateTime;

import ec.edu.espe.asignaciones.entities.AssignmentStatus;
import ec.edu.espe.asignaciones.entities.AssignmentType;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateAssignmentRequest {

    private AssignmentStatus status;

    private AssignmentType assignmentType;

    private OffsetDateTime validUntil;

    @Size(max = 80, message = "El alias del vehiculo no puede superar 80 caracteres")
    private String vehicleAlias;

    private Boolean entryAuthorized;

    @Size(max = 500, message = "La observacion no puede superar 500 caracteres")
    private String observation;

    @Size(max = 500, message = "El motivo del cambio no puede superar 500 caracteres")
    private String changeReason;
}
