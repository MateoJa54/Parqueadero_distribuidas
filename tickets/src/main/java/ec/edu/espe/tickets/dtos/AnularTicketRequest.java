package ec.edu.espe.tickets.dtos;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Peticion de anulacion. La anulacion corrige errores humanos del ingreso
 * (placa mal digitada, espacio equivocado, ingreso duplicado). Exige un motivo
 * para dejar evidencia; solo aplica a tickets ACTIVOS.
 */
@Data
public class AnularTicketRequest {

    @NotBlank(message = "El motivo de anulacion es obligatorio")
    private String motivo;
}
