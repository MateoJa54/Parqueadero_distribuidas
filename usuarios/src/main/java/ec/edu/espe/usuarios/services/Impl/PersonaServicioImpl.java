package ec.edu.espe.usuarios.services.Impl;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ec.edu.espe.usuarios.dtos.PersonaRequestDto;
import ec.edu.espe.usuarios.dtos.PersonaResponseDto;
import ec.edu.espe.usuarios.entidades.Persona;
import ec.edu.espe.usuarios.repositorios.PersonaRepositorio;
import ec.edu.espe.usuarios.repositorios.UsuarioRepositorio;
import ec.edu.espe.usuarios.services.PersonaServicio;
import ec.edu.espe.usuarios.services.UsuarioServicio;
import ec.edu.espe.usuarios.utils.RecursoNoEncontradoException;
import ec.edu.espe.usuarios.utils.ReglaNegocioException;
import ec.edu.espe.usuarios.utils.UtilMappers;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PersonaServicioImpl implements PersonaServicio {

    private final PersonaRepositorio personaRepositorio;
    private final UsuarioRepositorio usuarioRepositorio;
    private final UsuarioServicio usuarioServicio;
    private final UtilMappers mapper;

    @Override
    @Transactional(readOnly = true)
    public List<PersonaResponseDto> listarPersonas() {
        return personaRepositorio.findAll().stream()
                .map(mapper::toPersonaResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public PersonaResponseDto obtenerPersona(UUID idPersona) {
        Persona persona = personaRepositorio.findById(idPersona)
                .orElseThrow(() -> new RecursoNoEncontradoException("Persona no encontrada con ID: " + idPersona));
        return mapper.toPersonaResponse(persona);
    }

    @Override
    @Transactional
    public PersonaResponseDto crearPersona(PersonaRequestDto request) {

        String dni = request.getDni().trim();
        String email = request.getEmail().trim().toLowerCase();
        String phone = request.getPhone().trim();

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
                .firstName(request.getFirstName())
                .middleName(request.getMiddleName())
                .lastName(request.getLastName())
                .dni(dni)
                .email(email)
                .phone(phone)
                .address(request.getAddress())
                .nationality(request.getNationality())
                .active(true)
                .build();

        return mapper.toPersonaResponse(personaRepositorio.save(persona));
    }

    @Override
    @Transactional
    public PersonaResponseDto actualizarPersona(UUID idPersona, PersonaRequestDto request) {
        Persona persona = personaRepositorio.findById(idPersona)
                .orElseThrow(() -> new RecursoNoEncontradoException("Persona no encontrada con ID: " + idPersona));

        String dni = request.getDni().trim();
        String email = request.getEmail().trim().toLowerCase();
        String phone = request.getPhone().trim();

        if (personaRepositorio.existsByDniAndIdNot(dni, idPersona)) {
            throw new ReglaNegocioException("Ya existe una persona con el dni: " + dni);
        }
        if (personaRepositorio.existsByEmailIgnoreCaseAndIdNot(email, idPersona)) {
            throw new ReglaNegocioException("Ya existe una persona con el email: " + email);
        }
        if (personaRepositorio.existsByPhoneAndIdNot(phone, idPersona)) {
            throw new ReglaNegocioException("Ya existe una persona con el telefono: " + phone);
        }

        persona.setFirstName(request.getFirstName());
        persona.setMiddleName(request.getMiddleName());
        persona.setLastName(request.getLastName());
        persona.setDni(dni);
        persona.setEmail(email);
        persona.setPhone(phone);
        persona.setAddress(request.getAddress());
        persona.setNationality(request.getNationality());

        return mapper.toPersonaResponse(personaRepositorio.save(persona));
    }

    @Override
    @Transactional
    public PersonaResponseDto activarPersona(UUID idPersona) {
        Persona persona = personaRepositorio.findById(idPersona)
                .orElseThrow(() -> new RecursoNoEncontradoException("Persona no encontrada con ID: " + idPersona));

        persona.setActive(true);
        // No se reactiva el usuario en cascada: reactivar la identidad no implica
        // devolver el acceso. El usuario se reactiva de forma explicita (minimo privilegio).
        return mapper.toPersonaResponse(personaRepositorio.save(persona));
    }

    @Override
    @Transactional
    public PersonaResponseDto desactivarPersona(UUID idPersona) {
        Persona persona = personaRepositorio.findById(idPersona)
                .orElseThrow(() -> new RecursoNoEncontradoException("Persona no encontrada con ID: " + idPersona));

        // Cascada logica: revocar la identidad (persona) revoca el acceso del usuario,
        // y este, a su vez, retira sus roles. Se reutiliza la logica de UsuarioServicio
        // para no duplicar la cascada hacia las asignaciones.
        if (usuarioRepositorio.existsById(idPersona)) {
            usuarioServicio.desactivarUsuario(idPersona);
        }

        persona.setActive(false);
        return mapper.toPersonaResponse(personaRepositorio.save(persona));
    }

    @Override
    @Transactional(readOnly = true)
    public List<PersonaResponseDto> buscarPersonas(String dni, String nombre, String apellido) {
        String dniLimpio = dni != null ? dni.trim() : null;
        String nombreLimpio = nombre != null ? nombre.trim() : null;
        String apellidoLimpio = apellido != null ? apellido.trim() : null;

        boolean sinCriterios = (dniLimpio == null || dniLimpio.isEmpty())
                && (nombreLimpio == null || nombreLimpio.isEmpty())
                && (apellidoLimpio == null || apellidoLimpio.isEmpty());
        if (sinCriterios) {
            throw new IllegalArgumentException(
                    "Debe especificar al menos un criterio de busqueda: dni, nombre o apellido");
        }

        List<Persona> resultado;
        if (dniLimpio != null && !dniLimpio.isEmpty()) {
            resultado = personaRepositorio.findByDni(dniLimpio)
                    .map(List::of)
                    .orElseGet(List::of);
        } else if (apellidoLimpio != null && !apellidoLimpio.isEmpty()) {
            resultado = personaRepositorio.findByLastNameContainingIgnoreCase(apellidoLimpio);
        } else {
            resultado = personaRepositorio.findByFirstNameContainingIgnoreCase(nombreLimpio);
        }

        return resultado.stream()
                .map(mapper::toPersonaResponse)
                .collect(Collectors.toList());
    }
}
