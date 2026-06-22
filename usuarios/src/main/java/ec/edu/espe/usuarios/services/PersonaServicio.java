package ec.edu.espe.usuarios.services;

import java.util.List;
import java.util.UUID;

import ec.edu.espe.usuarios.dtos.PersonaRequestDto;
import ec.edu.espe.usuarios.dtos.PersonaResponseDto;

public interface PersonaServicio {

    List<PersonaResponseDto> listarPersonas();

    PersonaResponseDto obtenerPersona(UUID idPersona);

    PersonaResponseDto crearPersona(PersonaRequestDto request);

    PersonaResponseDto actualizarPersona(UUID idPersona, PersonaRequestDto request);

    PersonaResponseDto activarPersona(UUID idPersona);

    PersonaResponseDto desactivarPersona(UUID idPersona);

    // Busqueda flexible por cedula (exacta), nombre o apellido (parcial).
    // Al menos un criterio debe venir informado.
    List<PersonaResponseDto> buscarPersonas(String dni, String nombre, String apellido);
}
