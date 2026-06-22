package ec.edu.espe.usuarios.services.Impl;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ec.edu.espe.usuarios.dtos.AsignacionResponseDto;
import ec.edu.espe.usuarios.dtos.AsignarRolRequestDto;
import ec.edu.espe.usuarios.entidades.Rol;
import ec.edu.espe.usuarios.entidades.Usuario;
import ec.edu.espe.usuarios.entidades.UsuarioRol;
import ec.edu.espe.usuarios.entidades.UsuarioRolId;
import ec.edu.espe.usuarios.repositorios.RolRepositorio;
import ec.edu.espe.usuarios.repositorios.UsuarioRepositorio;
import ec.edu.espe.usuarios.repositorios.UsuarioRolRepositorio;
import ec.edu.espe.usuarios.services.AsignacionServicio;
import ec.edu.espe.usuarios.utils.RecursoNoEncontradoException;
import ec.edu.espe.usuarios.utils.ReglaNegocioException;
import ec.edu.espe.usuarios.utils.UtilMappers;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AsignacionServicioImpl implements AsignacionServicio {

    private final UsuarioRolRepositorio usuarioRolRepositorio;
    private final UsuarioRepositorio usuarioRepositorio;
    private final RolRepositorio rolRepositorio;
    private final UtilMappers mapper;

    @Override
    @Transactional
    public AsignacionResponseDto asignarRol(AsignarRolRequestDto request) {

        Usuario usuario = usuarioRepositorio.findById(request.getIdUser())
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Usuario no encontrado con ID: " + request.getIdUser()));

        Rol rol = rolRepositorio.findById(request.getIdRole())
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Rol no encontrado con ID: " + request.getIdRole()));

        if (!rol.isActive()) {
            throw new ReglaNegocioException("No se puede asignar un rol inactivo: " + rol.getName());
        }
        if (!usuario.isActive()) {
            throw new ReglaNegocioException("No se puede asignar roles a un usuario inactivo");
        }

        if (usuarioRolRepositorio.existsByUsuarioAndRol(usuario, rol)) {
            throw new ReglaNegocioException("El usuario ya tiene asignado el rol: " + rol.getName());
        }

        UsuarioRol asignacion = UsuarioRol.builder()
                .id(new UsuarioRolId(usuario.getId(), rol.getId()))
                .usuario(usuario)
                .rol(rol)
                .active(true)
                .build();

        return mapper.toAsignacionResponse(usuarioRolRepositorio.save(asignacion));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AsignacionResponseDto> listarAsignaciones() {
        return usuarioRolRepositorio.findAll().stream()
                .map(mapper::toAsignacionResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<AsignacionResponseDto> listarRolesDeUsuario(UUID idUsuario) {
        Usuario usuario = usuarioRepositorio.findById(idUsuario)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado con ID: " + idUsuario));
        return usuarioRolRepositorio.findByUsuario(usuario).stream()
                .map(mapper::toAsignacionResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public AsignacionResponseDto desactivarAsignacion(UUID idUsuario, UUID idRol) {
        UsuarioRol asignacion = usuarioRolRepositorio.findById(new UsuarioRolId(idUsuario, idRol))
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No existe asignacion para el usuario " + idUsuario + " y el rol " + idRol));

        asignacion.setActive(false);
        return mapper.toAsignacionResponse(usuarioRolRepositorio.save(asignacion));
    }

    @Override
    @Transactional
    public AsignacionResponseDto activarAsignacion(UUID idUsuario, UUID idRol) {
        UsuarioRol asignacion = usuarioRolRepositorio.findById(new UsuarioRolId(idUsuario, idRol))
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No existe asignacion para el usuario " + idUsuario + " y el rol " + idRol));

        // No tiene sentido reactivar una relacion si el rol o el usuario estan inactivos.
        if (!asignacion.getRol().isActive()) {
            throw new ReglaNegocioException("No se puede reactivar la asignacion: el rol esta inactivo");
        }
        if (!asignacion.getUsuario().isActive()) {
            throw new ReglaNegocioException("No se puede reactivar la asignacion: el usuario esta inactivo");
        }

        asignacion.setActive(true);
        return mapper.toAsignacionResponse(usuarioRolRepositorio.save(asignacion));
    }
}
