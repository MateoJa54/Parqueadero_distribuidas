package ec.edu.espe.usuarios.dtos.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** Solicitud para obtener un nuevo access token a partir de un refresh token vigente. */
@Data
public class RefreshRequest {

    @NotBlank(message = "El refreshToken es obligatorio")
    private String refreshToken;
}
