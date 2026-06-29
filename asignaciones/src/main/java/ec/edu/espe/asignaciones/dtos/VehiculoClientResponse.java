package ec.edu.espe.asignaciones.dtos;

import java.util.UUID;

import lombok.Data;

@Data
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
