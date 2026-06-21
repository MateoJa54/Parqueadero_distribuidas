package ec.edu.espe.usuarios.repositorios;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import ec.edu.espe.usuarios.entidades.NombreRol;
import ec.edu.espe.usuarios.entidades.Rol;

public interface RolRepositorio extends JpaRepository<Rol, UUID> {

    boolean existsByName(NombreRol name);

    boolean existsByNameAndIdNot(NombreRol name, UUID id);

    Optional<Rol> findByName(NombreRol name);
}
