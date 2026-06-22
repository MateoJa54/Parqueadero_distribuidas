package ec.edu.espe.usuarios.services.Impl;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ec.edu.espe.usuarios.dtos.RolRequestDto;
import ec.edu.espe.usuarios.dtos.RolResponseDto;
import ec.edu.espe.usuarios.entidades.Rol;
import ec.edu.espe.usuarios.entidades.UsuarioRol;
import ec.edu.espe.usuarios.repositorios.RolRepositorio;
import ec.edu.espe.usuarios.repositorios.UsuarioRolRepositorio;
import ec.edu.espe.usuarios.services.RolServicio;
import ec.edu.espe.usuarios.utils.RecursoNoEncontradoException;
import ec.edu.espe.usuarios.utils.ReglaNegocioException;
import ec.edu.espe.usuarios.utils.UtilMappers;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RolServicioImpl implements RolServicio {

    private final RolRepositorio rolRepositorio;
    private final UsuarioRolRepositorio usuarioRolRepositorio;
    private final UtilMappers mapper;

    /**
     * Normaliza el nombre del rol: quita espacios extremos y lo pasa a MAYUSCULAS
     * para garantizar unicidad consistente (ej: " admin " -> "ADMIN").
     */
    private String normalizarNombre(String name) {
        return name == null ? null : name.trim().toUpperCase();
    }

    @Override
    @Transactional(readOnly = true)
    public List<RolResponseDto> listarRoles() {
        return rolRepositorio.findAll().stream()
                .map(mapper::toRolResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public RolResponseDto obtenerRol(UUID idRol) {
        Rol rol = rolRepositorio.findById(idRol)
                .orElseThrow(() -> new RecursoNoEncontradoException("Rol no encontrado con ID: " + idRol));
        return mapper.toRolResponse(rol);
    }

    @Override
    @Transactional
    public RolResponseDto crearRol(RolRequestDto request) {

        String nombre = normalizarNombre(request.getName());

        if (rolRepositorio.existsByName(nombre)) {
            throw new ReglaNegocioException("Ya existe un rol con el nombre: " + nombre);
        }

        Rol rol = Rol.builder()
                .name(nombre)
                .description(request.getDescription())
                .active(true)
                .build();

        return mapper.toRolResponse(rolRepositorio.save(rol));
    }

    @Override
    @Transactional
    public RolResponseDto actualizarRol(UUID idRol, RolRequestDto request) {
        Rol rol = rolRepositorio.findById(idRol)
                .orElseThrow(() -> new RecursoNoEncontradoException("Rol no encontrado con ID: " + idRol));

        String nombre = normalizarNombre(request.getName());

        if (rolRepositorio.existsByNameAndIdNot(nombre, idRol)) {
            throw new ReglaNegocioException("Ya existe un rol con el nombre: " + nombre);
        }

        rol.setName(nombre);
        rol.setDescription(request.getDescription());

        return mapper.toRolResponse(rolRepositorio.save(rol));
    }

    @Override
    @Transactional
    public RolResponseDto activarRol(UUID idRol) {
        Rol rol = rolRepositorio.findById(idRol)
                .orElseThrow(() -> new RecursoNoEncontradoException("Rol no encontrado con ID: " + idRol));

        rol.setActive(true);
        return mapper.toRolResponse(rolRepositorio.save(rol));
    }

    @Override
    @Transactional
    public RolResponseDto desactivarRol(UUID idRol) {
        Rol rol = rolRepositorio.findById(idRol)
                .orElseThrow(() -> new RecursoNoEncontradoException("Rol no encontrado con ID: " + idRol));

        // Guarda de negocio: no se puede desactivar un rol que tiene usuarios
        // activos asignados (mismo criterio que zonas con espacios OCUPADOS).
        boolean hayUsuariosActivos = usuarioRolRepositorio.findByRol(rol).stream()
                .anyMatch(UsuarioRol::isActive);
        if (hayUsuariosActivos) {
            throw new ReglaNegocioException(
                    "No se puede desactivar el rol: tiene usuarios activos asignados. "
                            + "Primero retire (desactive) esas asignaciones.");
        }

        rol.setActive(false);
        return mapper.toRolResponse(rolRepositorio.save(rol));
    }
}
