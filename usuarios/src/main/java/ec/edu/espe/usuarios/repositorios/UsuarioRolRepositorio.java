package ec.edu.espe.usuarios.repositorios;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import ec.edu.espe.usuarios.entidades.Rol;
import ec.edu.espe.usuarios.entidades.Usuario;
import ec.edu.espe.usuarios.entidades.UsuarioRol;
import ec.edu.espe.usuarios.entidades.UsuarioRolId;

public interface UsuarioRolRepositorio extends JpaRepository<UsuarioRol, UsuarioRolId> {

    boolean existsByUsuarioAndRol(Usuario usuario, Rol rol);

    List<UsuarioRol> findByUsuario(Usuario usuario);

    List<UsuarioRol> findByRol(Rol rol);
}
