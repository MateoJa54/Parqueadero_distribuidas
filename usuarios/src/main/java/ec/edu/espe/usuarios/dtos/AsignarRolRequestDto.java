package ec.edu.espe.usuarios.dtos;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Peticion para asignar un rol a un usuario (tabla "user_role").
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AsignarRolRequestDto {

    @NotNull(message = "El id del usuario es obligatorio")
    private UUID idUser;

    @NotNull(message = "El id del rol es obligatorio")
    private UUID idRole;
}
