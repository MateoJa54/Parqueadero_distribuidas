package ec.edu.espe.usuarios.services.Impl;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ec.edu.espe.usuarios.audit.AuditPublisher;
import ec.edu.espe.usuarios.dtos.UsuarioRequestDto;
import ec.edu.espe.usuarios.dtos.UsuarioResponseDto;
import ec.edu.espe.usuarios.dtos.UsuarioUpdateDto;
import ec.edu.espe.usuarios.entidades.Persona;
import ec.edu.espe.usuarios.entidades.Usuario;
import ec.edu.espe.usuarios.repositorios.PersonaRepositorio;
import ec.edu.espe.usuarios.repositorios.UsuarioRepositorio;
import ec.edu.espe.usuarios.repositorios.UsuarioRolRepositorio;
import ec.edu.espe.usuarios.services.UsuarioServicio;
import ec.edu.espe.usuarios.utils.PasswordUtil;
import ec.edu.espe.usuarios.utils.RecursoNoEncontradoException;
import ec.edu.espe.usuarios.utils.ReglaNegocioException;
import ec.edu.espe.usuarios.utils.UtilMappers;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UsuarioServicioImpl implements UsuarioServicio {

    private static final String ENTIDAD = "USUARIO";
    private static final String USUARIO_NOT_FOUND = "Usuario no encontrado con ID: ";
    private static final String AUDIT_UPDATE = "UPDATE";

    private final UsuarioRepositorio usuarioRepositorio;
    private final PersonaRepositorio personaRepositorio;
    private final UsuarioRolRepositorio usuarioRolRepositorio;
    private final UtilMappers mapper;
    private final AuditPublisher auditPublisher;

    @Override
    @Transactional(readOnly = true)
    public List<UsuarioResponseDto> listarUsuarios() {
        return usuarioRepositorio.findAll().stream()
                .map(mapper::toUsuarioResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public UsuarioResponseDto obtenerUsuario(UUID idUsuario) {
        Usuario usuario = usuarioRepositorio.findById(idUsuario)
                .orElseThrow(() -> new RecursoNoEncontradoException(USUARIO_NOT_FOUND + idUsuario));
        return mapper.toUsuarioResponse(usuario);
    }

    @Override
    @Transactional
    public UsuarioResponseDto crearUsuario(UsuarioRequestDto request) {

        Persona persona = personaRepositorio.findById(request.getIdPersona())
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Persona no encontrada con ID: " + request.getIdPersona()));

        // No se puede crear un usuario (acceso) sobre una persona (identidad) inactiva.
        if (!persona.isActive()) {
            throw new ReglaNegocioException(
                    "No se puede crear un usuario: la persona esta inactiva");
        }

        // La PK de users es compartida con persons (relacion 1 a 1),
        // por lo que una persona solo puede tener un usuario.
        if (usuarioRepositorio.existsById(persona.getId())) {
            throw new ReglaNegocioException("La persona ya tiene un usuario asociado");
        }

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

        Usuario usuarioGuardado = usuarioRepositorio.save(usuario);
        auditPublisher.publicar("CREATE", ENTIDAD, usuarioGuardado);
        return mapper.toUsuarioResponse(usuarioGuardado);
    }

    @Override
    @Transactional
    public UsuarioResponseDto actualizarUsuario(UUID idUsuario, UsuarioUpdateDto request) {
        Usuario usuario = usuarioRepositorio.findById(idUsuario)
                .orElseThrow(() -> new RecursoNoEncontradoException(USUARIO_NOT_FOUND + idUsuario));

        if (!usuario.getPersona().getId().equals(request.getIdPersona())) {
            throw new IllegalArgumentException("No se permite cambiar la persona asociada al usuario");
        }

        String username = request.getUsername().trim();
        if (usuarioRepositorio.existsByUsernameIgnoreCaseAndIdNot(username, idUsuario)) {
            throw new ReglaNegocioException("Ya existe un usuario con el username: " + username);
        }

        usuario.setUsername(username);

        // La contrasena es opcional en la actualizacion: solo se re-hashea si
        // el cliente envia una nueva. Si llega nula o vacia, se conserva la actual.
        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            usuario.setPasswordHash(PasswordUtil.hash(request.getPassword()));
        }

        Usuario usuarioActualizado = usuarioRepositorio.save(usuario);
        auditPublisher.publicar(AUDIT_UPDATE, ENTIDAD, usuarioActualizado);
        return mapper.toUsuarioResponse(usuarioActualizado);
    }

    @Override
    @Transactional
    public UsuarioResponseDto activarUsuario(UUID idUsuario) {
        Usuario usuario = usuarioRepositorio.findById(idUsuario)
                .orElseThrow(() -> new RecursoNoEncontradoException(USUARIO_NOT_FOUND + idUsuario));

        // No se puede dar acceso a un usuario cuya persona (identidad) esta inactiva.
        if (usuario.getPersona() != null && !usuario.getPersona().isActive()) {
            throw new ReglaNegocioException("No se puede activar el usuario: su persona esta inactiva");
        }

        usuario.setActive(true);
        Usuario usuarioActivado = usuarioRepositorio.save(usuario);
        auditPublisher.publicar(AUDIT_UPDATE, ENTIDAD, usuarioActivado);
        return mapper.toUsuarioResponse(usuarioActivado);
    }

    @Override
    @Transactional
    public UsuarioResponseDto desactivarUsuario(UUID idUsuario) {
        Usuario usuario = usuarioRepositorio.findById(idUsuario)
                .orElseThrow(() -> new RecursoNoEncontradoException(USUARIO_NOT_FOUND + idUsuario));

        // Cascada logica: revocar el acceso del usuario tambien retira sus roles activos.
        // (Composicion: el usuario "posee" sus asignaciones de rol.)
        usuarioRolRepositorio.findByUsuario(usuario).forEach(asignacion -> {
            if (asignacion.isActive()) {
                asignacion.setActive(false);
                usuarioRolRepositorio.save(asignacion);
            }
        });

        usuario.setActive(false);
        Usuario usuarioDesactivado = usuarioRepositorio.save(usuario);
        auditPublisher.publicar(AUDIT_UPDATE, ENTIDAD, usuarioDesactivado);
        return mapper.toUsuarioResponse(usuarioDesactivado);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UsuarioResponseDto> buscarUsuarios(String username) {
        String filtro = username != null ? username.trim() : null;
        if (filtro == null || filtro.isEmpty()) {
            throw new IllegalArgumentException("El parametro 'username' es obligatorio para la busqueda");
        }
        return usuarioRepositorio.findByUsernameContainingIgnoreCase(filtro).stream()
                .map(mapper::toUsuarioResponse)
                .toList();
    }
}
