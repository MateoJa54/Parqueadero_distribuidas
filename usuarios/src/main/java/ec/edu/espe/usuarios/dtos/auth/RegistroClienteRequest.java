package ec.edu.espe.usuarios.dtos.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Auto-registro de cliente por VERIFICACION DE IDENTIDAD (sin exponer el
 * listado de personas). El cliente demuestra ser el titular aportando dos
 * factores que solo el deberia conocer: su {@code dni} y su {@code email}.
 *
 * El backend resuelve internamente la persona (nunca se envia ni se devuelve
 * el idPersona). Ante cualquier discrepancia se responde un mensaje GENERICO
 * para evitar enumeracion de cedulas/correos.
 */
@Data
public class RegistroClienteRequest {

    @NotBlank(message = "La cedula es obligatoria")
    @Pattern(regexp = "^\\d{10}$", message = "La cedula debe tener 10 digitos")
    private String dni;

    @NotBlank(message = "El correo es obligatorio")
    @jakarta.validation.constraints.Email(message = "El correo no es valido")
    @Size(max = 50, message = "El correo no puede exceder 50 caracteres")
    private String email;

    @NotBlank(message = "El username es obligatorio")
    @Size(min = 3, max = 15, message = "El username debe tener entre 3 y 15 caracteres")
    @Pattern(regexp = "^[a-zA-Z0-9._-]+$", message = "El username solo puede contener letras, numeros, punto, guion y guion bajo (sin espacios)")
    private String username;

    @NotBlank(message = "La contrasena es obligatoria")
    @Size(min = 6, max = 30, message = "La contrasena debe tener entre 6 y 30 caracteres")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).+$", message = "La contrasena debe incluir al menos una mayuscula, una minuscula y un numero")
    private String password;
}
