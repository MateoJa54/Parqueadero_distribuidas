package ec.edu.espe.usuarios.dtos.auth;

import java.util.List;
import java.util.UUID;

import lombok.Builder;
import lombok.Data;

/** Datos del usuario autenticado (endpoint "ver mis datos"). */
@Data
@Builder
public class PerfilResponse {

    private UUID idUsuario;
    private String username;
    private String nombreCompleto;
    private boolean active;
    private List<String> roles;
}
