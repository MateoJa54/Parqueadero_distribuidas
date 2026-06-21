package ec.edu.espe.usuarios.utils;

import org.springframework.stereotype.Component;

import ec.edu.espe.usuarios.dtos.AsignacionResponseDto;
import ec.edu.espe.usuarios.dtos.PersonaResponseDto;
import ec.edu.espe.usuarios.dtos.RolResponseDto;
import ec.edu.espe.usuarios.dtos.UsuarioResponseDto;
import ec.edu.espe.usuarios.entidades.Persona;
import ec.edu.espe.usuarios.entidades.Rol;
import ec.edu.espe.usuarios.entidades.Usuario;
import ec.edu.espe.usuarios.entidades.UsuarioRol;

@Component
public class UtilMappers {

    public PersonaResponseDto toPersonaResponse(Persona persona) {
        if (persona == null) {
            return null;
        }
        return PersonaResponseDto.builder()
                .id(persona.getId())
                .firstName(persona.getFirstName())
                .middleName(persona.getMiddleName())
                .lastName(persona.getLastName())
                .dni(persona.getDni())
                .email(persona.getEmail())
                .phone(persona.getPhone())
                .address(persona.getAddress())
                .nationality(persona.getNationality())
                .active(persona.isActive())
                .createdAt(persona.getCreatedAt())
                .updatedAt(persona.getUpdatedAt())
                .build();
    }

    public UsuarioResponseDto toUsuarioResponse(Usuario usuario) {
        if (usuario == null) {
            return null;
        }
        Persona persona = usuario.getPersona();
        String nombreCompleto = persona != null
                ? (persona.getFirstName() + " " + persona.getLastName())
                : null;
        return UsuarioResponseDto.builder()
                .id(usuario.getId())
                .idPersona(persona != null ? persona.getId() : null)
                .username(usuario.getUsername())
                .nombreCompleto(nombreCompleto)
                .active(usuario.isActive())
                .lastLogin(usuario.getLastLogin())
                .createdAt(usuario.getCreatedAt())
                .updatedAt(usuario.getUpdatedAt())
                .build();
    }

    public RolResponseDto toRolResponse(Rol rol) {
        if (rol == null) {
            return null;
        }
        return RolResponseDto.builder()
                .id(rol.getId())
                .name(rol.getName())
                .description(rol.getDescription())
                .active(rol.isActive())
                .createdAt(rol.getCreatedAt())
                .updatedAt(rol.getUpdatedAt())
                .build();
    }

    public AsignacionResponseDto toAsignacionResponse(UsuarioRol asignacion) {
        if (asignacion == null) {
            return null;
        }
        Usuario usuario = asignacion.getUsuario();
        Rol rol = asignacion.getRol();
        return AsignacionResponseDto.builder()
                .idUser(usuario != null ? usuario.getId() : null)
                .username(usuario != null ? usuario.getUsername() : null)
                .idRole(rol != null ? rol.getId() : null)
                .rol(rol != null ? rol.getName() : null)
                .active(asignacion.isActive())
                .assignedAt(asignacion.getAssignedAt())
                .updatedAt(asignacion.getUpdatedAt())
                .build();
    }
}
