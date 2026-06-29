package ec.edu.espe.usuarios.services.Impl;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ec.edu.espe.usuarios.dtos.AuthResponseDto;
import ec.edu.espe.usuarios.dtos.LoginRequestDto;
import ec.edu.espe.usuarios.dtos.PersonaRequestDto;
import ec.edu.espe.usuarios.dtos.PersonaResponseDto;
import ec.edu.espe.usuarios.dtos.RegisterRequestDto;
import ec.edu.espe.usuarios.dtos.UsuarioRequestDto;
import ec.edu.espe.usuarios.dtos.UsuarioResponseDto;
import ec.edu.espe.usuarios.entidades.Persona;
import ec.edu.espe.usuarios.entidades.Rol;
import ec.edu.espe.usuarios.entidades.Usuario;
import ec.edu.espe.usuarios.entidades.UsuarioRol;
import ec.edu.espe.usuarios.repositorios.PersonaRepositorio;
import ec.edu.espe.usuarios.repositorios.RolRepositorio;
import ec.edu.espe.usuarios.repositorios.UsuarioRepositorio;
import ec.edu.espe.usuarios.repositorios.UsuarioRolRepositorio;
import ec.edu.espe.usuarios.security.JwtProperties;
import ec.edu.espe.usuarios.security.JwtUtil;
import ec.edu.espe.usuarios.services.AuthService;
import ec.edu.espe.usuarios.utils.PasswordUtil;
import ec.edu.espe.usuarios.utils.RecursoNoEncontradoException;
import ec.edu.espe.usuarios.utils.ReglaNegocioException;
import ec.edu.espe.usuarios.utils.UtilMappers;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final String DEFAULT_GUEST_ROLE = "GUEST";
    private static final String DEFAULT_USER_ROLE = "CLIENTE";

    private final UsuarioRepositorio usuarioRepositorio;
    private final PersonaRepositorio personaRepositorio;
    private final RolRepositorio rolRepositorio;
    private final UsuarioRolRepositorio usuarioRolRepositorio;
    private final JwtUtil jwtUtil;
    private final UtilMappers mapper;
    private final JwtProperties jwtProperties;

    @Override
    @Transactional
    public AuthResponseDto login(LoginRequestDto request) {
        Usuario usuario = usuarioRepositorio.findByUsername(request.getUsername().trim())
                .orElseThrow(() -> new RecursoNoEncontradoException("Credenciales invalidas"));

        if (!usuario.isActive()) {
            throw new ReglaNegocioException("Usuario inactivo");
        }
        if (!PasswordUtil.matches(request.getPassword(), usuario.getPasswordHash())) {
            throw new ReglaNegocioException("Credenciales invalidas");
        }

        List<String> roles = usuarioRolRepositorio.findByUsuario(usuario).stream()
                .filter(UsuarioRol::isActive)
                .map(asignacion -> asignacion.getRol().getName())
                .collect(Collectors.toList());

        String token = jwtUtil.generateToken(usuario.getId(), usuario.getUsername(), roles);
        usuario.setLastLogin(LocalDateTime.now());
        usuarioRepositorio.save(usuario);

        return AuthResponseDto.builder()
                .token(token)
                .tokenType("Bearer")
                .userId(usuario.getId())
                .idPersona(usuario.getPersona().getId())
                .username(usuario.getUsername())
                .roles(roles)
                .active(usuario.isActive())
                .expiresAt(LocalDateTime.now().plusSeconds(jwtProperties.getExpirationSeconds()))
                .build();
    }

    @Override
    @Transactional
    public AuthResponseDto register(RegisterRequestDto request) {
        PersonaRequestDto personaRequest = request.getPersona();
        if (personaRequest == null) {
            throw new IllegalArgumentException("Los datos de la persona son obligatorios");
        }

        String dni = personaRequest.getDni().trim();
        String email = personaRequest.getEmail().trim().toLowerCase();
        String phone = personaRequest.getPhone().trim();

        if (personaRepositorio.existsByDni(dni)) {
            throw new ReglaNegocioException("Ya existe una persona con el dni: " + dni);
        }
        if (personaRepositorio.existsByEmailIgnoreCase(email)) {
            throw new ReglaNegocioException("Ya existe una persona con el email: " + email);
        }
        if (personaRepositorio.existsByPhone(phone)) {
            throw new ReglaNegocioException("Ya existe una persona con el telefono: " + phone);
        }

        Persona persona = Persona.builder()
                .firstName(personaRequest.getFirstName())
                .middleName(personaRequest.getMiddleName())
                .lastName(personaRequest.getLastName())
                .dni(dni)
                .email(email)
                .phone(phone)
                .address(personaRequest.getAddress())
                .nationality(personaRequest.getNationality())
                .active(true)
                .build();

        persona = personaRepositorio.save(persona);

        String username = request.getUsername().trim();
        if (usuarioRepositorio.existsByUsernameIgnoreCase(username)) {
            throw new ReglaNegocioException("Ya existe un usuario con el username: " + username);
        }

        Usuario usuario = Usuario.builder()
                .persona(persona)
                .username(username)
                .passwordHash(PasswordUtil.hash(request.getPassword()))
                .active(true)
                .build();
        usuario = usuarioRepositorio.save(usuario);

        Rol defaultRole = rolRepositorio.findByName(DEFAULT_USER_ROLE)
                .orElseThrow(() -> new RecursoNoEncontradoException("Rol por defecto no configurado: " + DEFAULT_USER_ROLE));

        UsuarioRol asignacion = UsuarioRol.builder()
                .id(new ec.edu.espe.usuarios.entidades.UsuarioRolId(usuario.getId(), defaultRole.getId()))
                .usuario(usuario)
                .rol(defaultRole)
                .active(true)
                .build();

        usuarioRolRepositorio.save(asignacion);

        List<String> roles = List.of(defaultRole.getName());
        String token = jwtUtil.generateToken(usuario.getId(), usuario.getUsername(), roles);

        return AuthResponseDto.builder()
                .token(token)
                .tokenType("Bearer")
                .userId(usuario.getId())
                .idPersona(persona.getId())
                .username(usuario.getUsername())
                .roles(roles)
                .active(usuario.isActive())
                .expiresAt(LocalDateTime.now().plusSeconds(jwtProperties.getExpirationSeconds()))
                .build();
    }
}
