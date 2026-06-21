package ec.edu.espe.usuarios.entidades;

import java.io.Serializable;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Clave primaria compuesta de la tabla intermedia "user_role"
 * (id_user, id_role), tal como aparece en el diagrama.
 */
@Embeddable
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UsuarioRolId implements Serializable {

    @Column(name = "id_user")
    private UUID idUser;

    @Column(name = "id_role")
    private UUID idRole;
}
