package ec.edu.espe.zonas.dtos;

import java.util.UUID;

import ec.edu.espe.zonas.entidades.EstadoEspacio;
import ec.edu.espe.zonas.entidades.TipoEspacio;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EspacioRequestDto {

    // El codigo NO se recibe del cliente: siempre lo autogenera el servicio.

    @NotNull(message = "El id de la zona es obligatorio")
    private UUID idZona;

    @Size(max = 150, message = "La descripción no puede superar 150 caracteres")
    private String descripcion;

    @NotNull(message = "El tipo de espacio es obligatorio")
    private TipoEspacio tipo;

    private EstadoEspacio estado;
}