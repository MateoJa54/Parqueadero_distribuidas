package ec.edu.espe.usuarios.services;

import java.util.List;
import java.util.UUID;

import ec.edu.espe.usuarios.dtos.RolRequestDto;
import ec.edu.espe.usuarios.dtos.RolResponseDto;

public interface RolServicio {

    List<RolResponseDto> listarRoles();

    RolResponseDto obtenerRol(UUID idRol);

    RolResponseDto crearRol(RolRequestDto request);

    RolResponseDto actualizarRol(UUID idRol, RolRequestDto request);

    RolResponseDto activarRol(UUID idRol);

    RolResponseDto desactivarRol(UUID idRol);
}
