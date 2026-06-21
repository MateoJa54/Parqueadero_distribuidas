package ec.edu.espe.zonas.dtos;

import java.util.UUID;

import ec.edu.espe.zonas.entidades.EstadoEspacio;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Respuesta compacta para consultar si un espacio puede usarse en este momento.
 * Pensada para que el futuro servicio de tickets verifique disponibilidad
 * con una sola llamada.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DisponibilidadResponseDto {

    private UUID idEspacio;
    private String codigo;
    private boolean disponible;
    private boolean activo;
    private EstadoEspacio estado;
}
