package ec.edu.espe.usuarios.services.Impl;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ec.edu.espe.usuarios.dtos.RolRequestDto;
import ec.edu.espe.usuarios.dtos.RolResponseDto;
import ec.edu.espe.usuarios.entidades.Rol;
import ec.edu.espe.usuarios.repositorios.RolRepositorio;
import ec.edu.espe.usuarios.services.RolServicio;
import ec.edu.espe.usuarios.utils.RecursoNoEncontradoException;
import ec.edu.espe.usuarios.utils.ReglaNegocioException;
import ec.edu.espe.usuarios.utils.UtilMappers;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RolServicioImpl implements RolServicio {

    private final RolRepositorio rolRepositorio;
    private final UtilMappers mapper;

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

        if (rolRepositorio.existsByName(request.getName())) {
            throw new ReglaNegocioException("Ya existe un rol con el nombre: " + request.getName());
        }

        Rol rol = Rol.builder()
                .name(request.getName())
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

        if (rolRepositorio.existsByNameAndIdNot(request.getName(), idRol)) {
            throw new ReglaNegocioException("Ya existe un rol con el nombre: " + request.getName());
        }

        rol.setName(request.getName());
        rol.setDescription(request.getDescription());

        return mapper.toRolResponse(rolRepositorio.save(rol));
    }

    @Override
    @Transactional
    public RolResponseDto eliminarRol(UUID idRol) {
        Rol rol = rolRepositorio.findById(idRol)
                .orElseThrow(() -> new RecursoNoEncontradoException("Rol no encontrado con ID: " + idRol));

        rol.setActive(false);
        return mapper.toRolResponse(rolRepositorio.save(rol));
    }
}
