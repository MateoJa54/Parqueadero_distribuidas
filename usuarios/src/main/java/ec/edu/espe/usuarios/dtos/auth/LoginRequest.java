package ec.edu.espe.usuarios.dtos.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** Credenciales para iniciar sesion. */
@Data
public class LoginRequest {

    @NotBlank(message = "El username es obligatorio")
    private String username;

    @NotBlank(message = "La contrasena es obligatoria")
    private String password;
}
