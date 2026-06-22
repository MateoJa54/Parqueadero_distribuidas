package ec.edu.espe.usuarios.services;

import java.util.List;
import java.util.UUID;

import ec.edu.espe.usuarios.dtos.AsignacionResponseDto;
import ec.edu.espe.usuarios.dtos.AsignarRolRequestDto;

public interface AsignacionServicio {

    AsignacionResponseDto asignarRol(AsignarRolRequestDto request);

    List<AsignacionResponseDto> listarAsignaciones();

    List<AsignacionResponseDto> listarRolesDeUsuario(UUID idUsuario);

    AsignacionResponseDto desactivarAsignacion(UUID idUsuario, UUID idRol);

    AsignacionResponseDto activarAsignacion(UUID idUsuario, UUID idRol);
}
