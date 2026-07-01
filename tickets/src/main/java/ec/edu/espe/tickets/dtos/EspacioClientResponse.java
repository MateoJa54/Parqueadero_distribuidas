package ec.edu.espe.tickets.dtos;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;

/** Proyeccion del espacio devuelto por el microservicio zonas. */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class EspacioClientResponse {

    private UUID id;
    private String codigo;
    private String descripcion;
    private String tipo;
    private boolean activo;
    private UUID idZona;
    private String nombreZona;
    private String estado;
}
