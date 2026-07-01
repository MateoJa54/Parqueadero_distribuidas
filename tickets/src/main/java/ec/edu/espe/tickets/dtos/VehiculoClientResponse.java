package ec.edu.espe.tickets.dtos;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;

/** Proyeccion del vehiculo devuelto por el microservicio vehiculos. */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class VehiculoClientResponse {

    private UUID id;
    private String placa;
    private String marca;
    private String modelo;
    private String color;
    private Integer anio;
    private String clasificacion;
    private String tipo;
    private boolean activo;
}
