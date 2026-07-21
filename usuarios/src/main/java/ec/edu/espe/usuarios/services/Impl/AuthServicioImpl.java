package ec.edu.espe.usuarios.services.Impl;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ec.edu.espe.usuarios.audit.AuditPublisher;
import ec.edu.espe.usuarios.dtos.AsignarRolRequestDto;
import ec.edu.espe.usuarios.dtos.PersonaRequestDto;
import ec.edu.espe.usuarios.dtos.PersonaResponseDto;
import ec.edu.espe.usuarios.dtos.UsuarioRequestDto;
import ec.edu.espe.usuarios.dtos.UsuarioResponseDto;
import ec.edu.espe.usuarios.dtos.auth.AuthResponse;
import ec.edu.espe.usuarios.dtos.auth.LoginRequest;
import ec.edu.espe.usuarios.dtos.auth.PerfilResponse;
import ec.edu.espe.usuarios.dtos.auth.RefreshRequest;
import ec.edu.espe.usuarios.dtos.auth.RegisterRequest;
import ec.edu.espe.usuarios.dtos.auth.RegistroClienteRequest;
import ec.edu.espe.usuarios.dtos.auth.RegistroCompletoRequest;
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
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
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

    private static final String ENTIDAD = "USUARIO";

    private final UsuarioRepositorio usuarioRepositorio;
    private final UsuarioRolRepositorio usuarioRolRepositorio;
    private final RolRepositorio rolRepositorio;
    private final ec.edu.espe.usuarios.repositorios.PersonaRepositorio personaRepositorio;
    private final UsuarioServicio usuarioServicio;
    private final ec.edu.espe.usuarios.services.PersonaServicio personaServicio;
    private final AsignacionServicio asignacionServicio;
    private final JwtService jwtService;
    private final AuditPublisher auditPublisher;

    @Lazy
    @Autowired
    private AuthServicioImpl self;

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

        usuario.setLastLogin(LocalDateTime.now(ZoneId.of("America/Guayaquil")));
        Usuario usuarioActualizado = usuarioRepositorio.save(usuario);

        // En este punto todavia no existe una sesion autenticada (el login ES
        // el proceso de conseguirla), asi que AuditPublisher no tiene forma de
        // inferir el actor desde el JWT/SecurityContext. Se pasa explicito.
        List<String> roles = rolesActivos(usuarioActualizado);
        auditPublisher.publicar("LOGIN", ENTIDAD, usuarioActualizado,
                usuarioActualizado.getUsername(), roles.isEmpty() ? null : roles.get(0));

        return construirRespuesta(usuarioActualizado, roles);
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
    @Transactional
    public AuthResponse registrarCliente(RegistroClienteRequest request) {
        // Mensaje UNICO y generico para TODOS los casos de fallo (persona inexistente,
        // email que no coincide, persona inactiva o cuenta ya creada). Asi el endpoint
        // no permite enumerar cedulas/correos ni saber si una cedula existe en el sistema.
        final String generico = "No pudimos verificar tu identidad con esos datos, "
                + "o ya existe una cuenta asociada. Verifica tu cedula y correo, o contacta al administrador.";

        Persona persona = personaRepositorio.findByDni(request.getDni().trim())
                .orElseThrow(() -> new ReglaNegocioException(generico));

        // Segundo factor de identidad: el correo debe coincidir exactamente (sin distinguir mayusculas).
        if (persona.getEmail() == null || !persona.getEmail().equalsIgnoreCase(request.getEmail().trim())) {
            throw new ReglaNegocioException(generico);
        }
        // La persona debe estar activa y no tener aun un usuario (PK compartida persona<->usuario).
        if (!persona.isActive() || usuarioRepositorio.existsById(persona.getId())) {
            throw new ReglaNegocioException(generico);
        }

        // Identidad verificada: reutiliza el alta de usuario + rol CLIENTE con el idPersona resuelto.
        RegisterRequest interno = new RegisterRequest();
        interno.setIdPersona(persona.getId());
        interno.setUsername(request.getUsername());
        interno.setPassword(request.getPassword());
        return self.register(interno);
    }

    @Override
    @Transactional
    public AuthResponse registrarCompleto(RegistroCompletoRequest request) {
        // 1) Crea la persona (identidad) reutilizando PersonaServicio, que valida
        //    cedula ecuatoriana + unicidad de dni/email/telefono y la deja activa.
        PersonaResponseDto persona = personaServicio.crearPersona(PersonaRequestDto.builder()
                .firstName(request.getFirstName())
                .middleName(request.getMiddleName())
                .lastName(request.getLastName())
                .dni(request.getDni())
                .email(request.getEmail())
                .phone(request.getPhone())
                .address(request.getAddress())
                .nationality(request.getNationality())
                .build());

        // 2) Crea el usuario (acceso) sobre esa persona + rol CLIENTE. Al compartir la
        //    transaccion, si falla el alta del usuario tambien se revierte la persona.
        RegisterRequest interno = new RegisterRequest();
        interno.setIdPersona(persona.getId());
        interno.setUsername(request.getUsername());
        interno.setPassword(request.getPassword());
        return self.register(interno);
    }

    @Override
    @Transactional
    public AuthResponse refrescar(RefreshRequest request) {
        // 1. Validar firma, expiracion y que el token sea realmente de tipo refresh.
        Claims claims;
        try {
            claims = jwtService.validarTipo(request.getRefreshToken().trim(), JwtService.TYPE_REFRESH);
        } catch (JwtException | IllegalArgumentException ex) {
            throw new CredencialesInvalidasException("Refresh token invalido o expirado: inicie sesion de nuevo");
        }

        // 2. El usuario debe seguir existiendo y estar activo.
        UUID idUsuario;
        try {
            idUsuario = UUID.fromString(claims.getSubject());
        } catch (IllegalArgumentException ex) {
            throw new CredencialesInvalidasException("Refresh token invalido o expirado: inicie sesion de nuevo");
        }
        Usuario usuario = usuarioRepositorio.findById(idUsuario)
                .orElseThrow(() -> new CredencialesInvalidasException(
                        "Refresh token invalido o expirado: inicie sesion de nuevo"));
        if (!usuario.isActive()) {
            throw new CredencialesInvalidasException("La cuenta esta inactiva");
        }

        // 3. Emitir un nuevo access y ROTAR el refresh (roles frescos desde la BD).
        return construirRespuesta(usuario, rolesActivos(usuario));
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
        String refreshToken = jwtService.generarRefreshToken(usuario.getId(), usuario.getUsername());
        return AuthResponse.builder()
                .token(token)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtService.getExpirationSeconds())
                .refreshExpiresIn(jwtService.getRefreshExpirationSeconds())
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
