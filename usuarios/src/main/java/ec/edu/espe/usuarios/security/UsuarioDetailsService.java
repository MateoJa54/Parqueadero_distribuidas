package ec.edu.espe.usuarios.security;

import java.util.List;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import ec.edu.espe.usuarios.entidades.Usuario;
import ec.edu.espe.usuarios.repositorios.UsuarioRepositorio;
import ec.edu.espe.usuarios.repositorios.UsuarioRolRepositorio;
import ec.edu.espe.usuarios.utils.RecursoNoEncontradoException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UsuarioDetailsService implements UserDetailsService {

    private final UsuarioRepositorio usuarioRepositorio;
    private final UsuarioRolRepositorio usuarioRolRepositorio;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Usuario usuario = usuarioRepositorio.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado"));

        List<String> roles = usuarioRolRepositorio.findByUsuario(usuario).stream()
                .filter(asignacion -> asignacion.isActive() && asignacion.getRol().isActive())
                .map(asignacion -> asignacion.getRol().getName())
                .toList();

        return new CustomUserDetails(usuario, roles);
    }
}
