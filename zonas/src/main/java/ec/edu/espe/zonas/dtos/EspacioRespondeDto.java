package ec.edu.espe.zonas.dtos;

import java.util.UUID;

import ec.edu.espe.zonas.entidades.EstadoEspacio;
import ec.edu.espe.zonas.entidades.TipoEspacio;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EspacioRespondeDto {

    private UUID id;

    private String codigo;

    private String descripcion;

    private TipoEspacio tipo;

    private boolean activo;

    private UUID idZona;

    private String nombreZona;
    
    private EstadoEspacio estado;
}
