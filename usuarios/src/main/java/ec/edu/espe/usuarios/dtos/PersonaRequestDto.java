package ec.edu.espe.usuarios.dtos;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import ec.edu.espe.usuarios.validaciones.CedulaEcuatoriana;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PersonaRequestDto {

    @NotBlank(message = "El primer nombre es obligatorio")
    @Size(max = 30, message = "El primer nombre no puede tener mas de 30 caracteres")
    @Pattern(regexp = "^[\\p{L} ]+$", message = "El primer nombre solo puede contener letras y espacios")
    private String firstName;

    @Size(max = 30, message = "El segundo nombre no puede tener mas de 30 caracteres")
    @Pattern(regexp = "^[\\p{L} ]*$", message = "El segundo nombre solo puede contener letras y espacios")
    private String middleName;

    @NotBlank(message = "El apellido es obligatorio")
    @Size(max = 30, message = "El apellido no puede tener mas de 30 caracteres")
    @Pattern(regexp = "^[\\p{L} ]+$", message = "El apellido solo puede contener letras y espacios")
    private String lastName;

    @NotBlank(message = "El dni es obligatorio")
    @CedulaEcuatoriana(message = "El dni debe ser una cedula ecuatoriana valida de 10 digitos")
    private String dni;

    @NotBlank(message = "El email es obligatorio")
    @Email(message = "El email no tiene un formato valido")
    @Size(max = 50, message = "El email no puede tener mas de 50 caracteres")
    private String email;

    @NotBlank(message = "El telefono es obligatorio")
    @Pattern(regexp = "^\\d{7,10}$", message = "El telefono debe tener entre 7 y 10 digitos numericos")
    private String phone;

    @Size(max = 255, message = "La direccion no puede tener mas de 255 caracteres")
    private String address;

    @NotBlank(message = "La nacionalidad es obligatoria")
    @Size(max = 30, message = "La nacionalidad no puede tener mas de 30 caracteres")
    @Pattern(regexp = "^[\\p{L} ]+$", message = "La nacionalidad solo puede contener letras y espacios")
    private String nationality;
}
