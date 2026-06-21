package ec.edu.espe.usuarios.dtos;

import ec.edu.espe.usuarios.entidades.NombreRol;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RolRequestDto {

    @NotNull(message = "El nombre del rol es obligatorio (ADMINISTRADOR, SUPERVISOR, OPERADOR, CAJERO, CLIENTE)")
    private NombreRol name;

    @Size(max = 255, message = "La descripcion no puede tener mas de 255 caracteres")
    private String description;
}
