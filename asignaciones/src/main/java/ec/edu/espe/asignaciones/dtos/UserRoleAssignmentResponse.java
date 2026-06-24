package ec.edu.espe.asignaciones.dtos;

import java.util.UUID;

import lombok.Data;

@Data
public class UserRoleAssignmentResponse {

    private UUID idUser;
    private String username;
    private UUID idRole;
    private String rol;
    private boolean active;
}
