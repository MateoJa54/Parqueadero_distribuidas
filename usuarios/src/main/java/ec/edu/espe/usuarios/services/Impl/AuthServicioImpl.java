package ec.edu.espe.usuarios.services.Impl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ec.edu.espe.usuarios.dtos.AsignarRolRequestDto;
import ec.edu.espe.usuarios.dtos.UsuarioRequestDto;
import ec.edu.espe.usuarios.dtos.UsuarioResponseDto;
import ec.edu.espe.usuarios.dtos.auth.AuthResponse;
import ec.edu.espe.usuarios.dtos.auth.LoginRequest;
import ec.edu.espe.usuarios.dtos.auth.PerfilResponse;
import ec.edu.espe.usuarios.dtos.auth.RegisterRequest;
import ec.edu.espe.usuarios.entidades.Persona;
import ec.edu.espe.usuarios.entidades.Rol;
import ec.edu.espe.usuarios.entidades.Usuario;
import ec.edu.espe.usuarios.repositorios.RolRepositorio;
import ec.edu.espe.usuarios.repositorios.UsuarioRepositorio;
import ec.edu.espe.usuarios.repositorios.UsuarioRolRepositorio;
import ec.edu.espe.usuarios.security.JwtService;
import ec.edu.espe.usuarios.security.RolesBase;
import ec.edu.espe.usuarios.services.AsignacionServicio;
import ec.edu.espe.usuarios.services.AuthServicio;
import ec.edu.espe.usuarios.services.UsuarioServicio;
import ec.edu.espe.usuarios.utils.CredencialesInvalidasException;
import ec.edu.espe.usuarios.utils.PasswordUtil;
import ec.edu.espe.usuarios.utils.RecursoNoEncontradoException;
import ec.edu.espe.usuarios.utils.ReglaNegocioException;
import lombok.RequiredArgsConstructor;

/**
 * Implementa la autenticacion stateless del sistema.
 *
 * usuarios es el unico emisor de JWT: ningun otro microservicio crea tokens.
 * El registro reutiliza la logica de creacion de usuario y le asigna el rol
 * CLIENTE por defecto, dentro de una unica transaccion.
 */
@Service
@RequiredArgsConstructor
public class AuthServicioImpl implements AuthServicio {

    private final UsuarioRepositorio usuarioRepositorio;
    private final UsuarioRolRepositorio usuarioRolRepositorio;
    private final RolRepositorio rolRepositorio;
    private final UsuarioServicio usuarioServicio;
    private final AsignacionServicio asignacionServicio;
    private final JwtService jwtService;

    @Override
    @Transactional
    public AuthResponse login(LoginRequest request) {
        // Mensaje generico para no revelar si fallo el username o la contrasena.
        Usuario usuario = usuarioRepositorio.findByUsernameIgnoreCase(request.getUsername().trim())
                .orElseThrow(() -> new CredencialesInvalidasException("Usuario o contrasena incorrectos"));

        if (!usuario.isActive()) {
            throw new CredencialesInvalidasException("Usuario o contrasena incorrectos");
        }
        if (!PasswordUtil.matches(request.getPassword(), usuario.getPasswordHash())) {
            throw new CredencialesInvalidasException("Usuario o contrasena incorrectos");
        }

        usuario.setLastLogin(LocalDateTime.now());
        usuarioRepositorio.save(usuario);

        return construirRespuesta(usuario, rolesActivos(usuario));
    }

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        // 1. Crear el acceso (usuario) sobre una persona existente. Reutiliza las
        //    validaciones de UsuarioServicio (persona activa, sin usuario previo, etc.).
        UsuarioResponseDto creado = usuarioServicio.crearUsuario(UsuarioRequestDto.builder()
                .idPersona(request.getIdPersona())
                .username(request.getUsername())
                .password(request.getPassword())
                .build());

        // 2. Asignar el rol CLIENTE por defecto (sembrado por RolesSeeder).
        Rol cliente = rolRepositorio.findByName(RolesBase.CLIENTE)
                .orElseThrow(() -> new ReglaNegocioException(
                        "El rol base CLIENTE no existe; no se pudo completar el registro"));

        asignacionServicio.asignarRol(AsignarRolRequestDto.builder()
                .idUser(creado.getId())
                .idRole(cliente.getId())
                .build());

        Usuario usuario = usuarioRepositorio.findById(creado.getId())
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Usuario no encontrado tras el registro: " + creado.getId()));

        return construirRespuesta(usuario, List.of(RolesBase.CLIENTE));
    }

    @Override
    @Transactional(readOnly = true)
    public PerfilResponse perfil(UUID idUsuario) {
        Usuario usuario = usuarioRepositorio.findById(idUsuario)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado con ID: " + idUsuario));

        return PerfilResponse.builder()
                .idUsuario(usuario.getId())
                .username(usuario.getUsername())
                .nombreCompleto(nombreCompleto(usuario.getPersona()))
                .active(usuario.isActive())
                .roles(rolesActivos(usuario))
                .build();
    }

    /** Nombres de los roles ACTIVOS del usuario; son los que viajan en el token. */
    private List<String> rolesActivos(Usuario usuario) {
        return usuarioRolRepositorio.findByUsuario(usuario).stream()
                .filter(asignacion -> asignacion.isActive())
                .map(asignacion -> asignacion.getRol().getName())
                .toList();
    }

    private AuthResponse construirRespuesta(Usuario usuario, List<String> roles) {
        String token = jwtService.generarToken(usuario.getId(), usuario.getUsername(), roles);
        return AuthResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .expiresIn(jwtService.getExpirationSeconds())
                .idUsuario(usuario.getId())
                .username(usuario.getUsername())
                .roles(roles)
                .build();
    }

    private String nombreCompleto(Persona persona) {
        if (persona == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder(persona.getFirstName());
        if (persona.getMiddleName() != null && !persona.getMiddleName().isBlank()) {
            sb.append(' ').append(persona.getMiddleName());
        }
        sb.append(' ').append(persona.getLastName());
        return sb.toString();
    }
}
