package ec.edu.espe.usuarios.repositorios;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import ec.edu.espe.usuarios.entidades.Usuario;

public interface UsuarioRepositorio extends JpaRepository<Usuario, UUID> {

    boolean existsByUsernameIgnoreCase(String username);

    boolean existsByUsernameIgnoreCaseAndIdNot(String username, UUID id);

    Optional<Usuario> findByUsername(String username);

    // Busqueda parcial de usuarios por username (para localizar al operador del ticket).
    List<Usuario> findByUsernameContainingIgnoreCase(String username);
}
