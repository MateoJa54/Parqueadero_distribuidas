package ec.edu.espe.tickets.dtos;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Peticion de ingreso: registrar la entrada de un vehiculo a un espacio.
 *
 * <p>La identificacion es SOLO por placa (cada vehiculo tiene, como maximo, una
 * asignacion activa; el usuario se resuelve automaticamente desde asignaciones).
 * El empleado no se envia: se toma del JWT de la sesion.
 */
@Data
public class RegistrarIngresoRequest {

    @NotBlank(message = "La placa es obligatoria")
    private String placa;

    @NotNull(message = "El id del espacio es obligatorio")
    private UUID idEspacio;
}
