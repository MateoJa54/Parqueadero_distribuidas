package ec.edu.espe.usuarios.dtos.auth;

import java.util.List;
import java.util.UUID;

import lombok.Builder;
import lombok.Data;

/** Respuesta de login/register: el token y los datos basicos de la sesion. */
@Data
@Builder
public class AuthResponse {

    private String token;
    private String refreshToken;
    private String tokenType;
    private long expiresIn;
    private long refreshExpiresIn;
    private UUID idUsuario;
    private String username;
    private List<String> roles;
}
