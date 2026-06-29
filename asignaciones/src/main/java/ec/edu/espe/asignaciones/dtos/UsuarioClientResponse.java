package ec.edu.espe.asignaciones.dtos;

import java.util.UUID;

import lombok.Data;

@Data
public class UsuarioClientResponse {

    private UUID id;
    private String username;
    private String nombreCompleto;
    private boolean active;
}
