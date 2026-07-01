package ec.edu.espe.asignaciones.dtos;

import java.util.UUID;

import ec.edu.espe.asignaciones.entities.AssignmentType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateAssignmentRequest {

    @NotNull(message = "El userId es obligatorio")
    private UUID userId;

    @NotNull(message = "El vehicleId es obligatorio")
    private UUID vehicleId;

    private AssignmentType assignmentType = AssignmentType.PROPIETARIO;

    @Size(max = 80, message = "El alias del vehiculo no puede superar 80 caracteres")
    private String vehicleAlias;

    @Size(max = 500, message = "La observacion no puede superar 500 caracteres")
    private String observation;
}
