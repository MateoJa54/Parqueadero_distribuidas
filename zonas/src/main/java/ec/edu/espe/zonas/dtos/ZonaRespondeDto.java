package ec.edu.espe.zonas.dtos;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import ec.edu.espe.zonas.entidades.TipoZona;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ZonaRespondeDto {

    private UUID idZona;

    private String nombre;

    private String codigo;

    private String descripcion;

    private boolean activo;

    private TipoZona tipoZona;

    private List<EspacioRespondeDto> espacios;

    private LocalDateTime fechaCreacion;

    private LocalDateTime fechaActualizacion;

    private int capacidad;
}
