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
    private UUID idPersona;
    private String username;
    private String nombreCompleto;

    // Datos personales del propio usuario (el CLIENTE puede ver los suyos aqui,
    // sin acceder al endpoint de personas restringido a ADMIN/ROOT).
    private String firstName;
    private String middleName;
    private String lastName;
    private String dni;
    private String email;
    private String phone;
    private String address;
    private String nationality;

    private boolean active;
    private List<String> roles;
}
