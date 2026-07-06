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

/**
 * DTO para actualizar un usuario.
 *
 * A diferencia de {@link UsuarioRequestDto} (creación), aquí el {@code password}
 * es <strong>opcional</strong>: si llega nulo o vacío, se conserva la contraseña
 * actual; si llega con valor, debe cumplir las mismas reglas de complejidad.
 * Como {@code @Size}/{@code @Pattern} no validan valores nulos, el campo solo se
 * valida cuando el cliente realmente envía una nueva contraseña.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UsuarioUpdateDto {

    @NotNull(message = "El id de la persona es obligatorio")
    private UUID idPersona;

    @NotBlank(message = "El username es obligatorio")
    @Size(min = 3, max = 15, message = "El username debe tener entre 3 y 15 caracteres")
    @Pattern(regexp = "^[a-zA-Z0-9._-]+$", message = "El username solo puede contener letras, numeros, punto, guion y guion bajo (sin espacios)")
    private String username;

    // El password es OPCIONAL en la edicion: si llega nulo o vacio se conserva
    // la contrasena actual (el servicio lo trata asi). El patron admite la
    // cadena vacia ("") o, si se envia una contrasena, exige 6-30 caracteres con
    // al menos una mayuscula, una minuscula y un numero. Asi la validacion honra
    // el contrato documentado en vez de rechazar el valor vacio.
    @Pattern(
            regexp = "^$|^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{6,30}$",
            message = "Si se envia, la contrasena debe tener entre 6 y 30 caracteres e incluir al menos una mayuscula, una minuscula y un numero")
    private String password;
}
