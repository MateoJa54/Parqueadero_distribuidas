package ec.edu.espe.usuarios.dtos;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UsuarioRequestDto {

    @NotNull(message = "El id de la persona es obligatorio")
    private UUID idPersona;

    @NotBlank(message = "El username es obligatorio")
    @Size(min = 3, max = 15, message = "El username debe tener entre 3 y 15 caracteres")
    @Pattern(regexp = "^[a-zA-Z0-9._-]+$", message = "El username solo puede contener letras, numeros, punto, guion y guion bajo (sin espacios)")
    private String username;

    @NotBlank(message = "La contrasena es obligatoria")
    @Size(min = 6, max = 30, message = "La contrasena debe tener entre 6 y 30 caracteres")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).+$", message = "La contrasena debe incluir al menos una mayuscula, una minuscula y un numero")
    private String password;
}
