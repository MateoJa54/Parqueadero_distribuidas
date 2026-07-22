package ec.edu.espe.usuarios.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.LocalDateTime;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ec.edu.espe.usuarios.dtos.AsignacionResponseDto;
import ec.edu.espe.usuarios.dtos.PersonaResponseDto;
import ec.edu.espe.usuarios.dtos.RolResponseDto;
import ec.edu.espe.usuarios.dtos.UsuarioResponseDto;
import ec.edu.espe.usuarios.entidades.Persona;
import ec.edu.espe.usuarios.entidades.Rol;
import ec.edu.espe.usuarios.entidades.Usuario;
import ec.edu.espe.usuarios.entidades.UsuarioRol;
import ec.edu.espe.usuarios.entidades.UsuarioRolId;

class UtilMappersTest {

    private final UtilMappers mapper = new UtilMappers();

    private Persona persona(UUID id) {
        return Persona.builder()
                .id(id)
                .firstName("Ana")
                .middleName("Maria")
                .lastName("Perez")
                .dni("1712345678")
                .email("ana@example.com")
                .phone("0999999999")
                .address("Quito")
                .nationality("EC")
                .active(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("toPersonaResponse devuelve null si la persona es null")
    void toPersonaResponseNull() {
        assertNull(mapper.toPersonaResponse(null));
    }

    @Test
    @DisplayName("toPersonaResponse mapea todos los campos")
    void toPersonaResponseMapea() {
        UUID id = UUID.randomUUID();
        PersonaResponseDto dto = mapper.toPersonaResponse(persona(id));

        assertNotNull(dto);
        assertEquals(id, dto.getId());
        assertEquals("Ana", dto.getFirstName());
        assertEquals("Maria", dto.getMiddleName());
        assertEquals("Perez", dto.getLastName());
        assertEquals("1712345678", dto.getDni());
        assertEquals("ana@example.com", dto.getEmail());
        assertEquals("0999999999", dto.getPhone());
        assertEquals("Quito", dto.getAddress());
        assertEquals("EC", dto.getNationality());
    }

    @Test
    @DisplayName("toUsuarioResponse devuelve null si el usuario es null")
    void toUsuarioResponseNull() {
        assertNull(mapper.toUsuarioResponse(null));
    }

    @Test
    @DisplayName("toUsuarioResponse arma nombreCompleto cuando hay persona")
    void toUsuarioResponseConPersona() {
        UUID idUsuario = UUID.randomUUID();
        UUID idPersona = UUID.randomUUID();
        Usuario usuario = Usuario.builder()
                .id(idUsuario)
                .persona(persona(idPersona))
                .username("aperez")
                .active(true)
                .build();

        UsuarioResponseDto dto = mapper.toUsuarioResponse(usuario);

        assertNotNull(dto);
        assertEquals(idUsuario, dto.getId());
        assertEquals(idPersona, dto.getIdPersona());
        assertEquals("aperez", dto.getUsername());
        assertEquals("Ana Perez", dto.getNombreCompleto());
    }

    @Test
    @DisplayName("toUsuarioResponse deja nombreCompleto e idPersona null cuando no hay persona")
    void toUsuarioResponseSinPersona() {
        Usuario usuario = Usuario.builder()
                .id(UUID.randomUUID())
                .username("aperez")
                .active(false)
                .build();

        UsuarioResponseDto dto = mapper.toUsuarioResponse(usuario);

        assertNotNull(dto);
        assertNull(dto.getIdPersona());
        assertNull(dto.getNombreCompleto());
    }

    @Test
    @DisplayName("toRolResponse devuelve null si el rol es null")
    void toRolResponseNull() {
        assertNull(mapper.toRolResponse(null));
    }

    @Test
    @DisplayName("toRolResponse mapea los campos del rol")
    void toRolResponseMapea() {
        UUID id = UUID.randomUUID();
        Rol rol = Rol.builder()
                .id(id)
                .name("ADMIN")
                .description("Administrador")
                .active(true)
                .build();

        RolResponseDto dto = mapper.toRolResponse(rol);

        assertNotNull(dto);
        assertEquals(id, dto.getId());
        assertEquals("ADMIN", dto.getName());
        assertEquals("Administrador", dto.getDescription());
    }

    @Test
    @DisplayName("toAsignacionResponse devuelve null si la asignacion es null")
    void toAsignacionResponseNull() {
        assertNull(mapper.toAsignacionResponse(null));
    }

    @Test
    @DisplayName("toAsignacionResponse mapea usuario y rol cuando estan presentes")
    void toAsignacionResponseCompleto() {
        UUID idUsuario = UUID.randomUUID();
        UUID idRol = UUID.randomUUID();
        Usuario usuario = Usuario.builder().id(idUsuario).username("aperez").active(true).build();
        Rol rol = Rol.builder().id(idRol).name("ADMIN").active(true).build();
        UsuarioRol asignacion = UsuarioRol.builder()
                .id(new UsuarioRolId(idUsuario, idRol))
                .usuario(usuario)
                .rol(rol)
                .active(true)
                .build();

        AsignacionResponseDto dto = mapper.toAsignacionResponse(asignacion);

        assertNotNull(dto);
        assertEquals(idUsuario, dto.getIdUser());
        assertEquals("aperez", dto.getUsername());
        assertEquals(idRol, dto.getIdRole());
        assertEquals("ADMIN", dto.getRol());
    }

    @Test
    @DisplayName("toAsignacionResponse es null-safe cuando usuario y rol son null")
    void toAsignacionResponseSinRelaciones() {
        UsuarioRol asignacion = UsuarioRol.builder()
                .id(new UsuarioRolId(UUID.randomUUID(), UUID.randomUUID()))
                .active(false)
                .build();

        AsignacionResponseDto dto = mapper.toAsignacionResponse(asignacion);

        assertNotNull(dto);
        assertNull(dto.getIdUser());
        assertNull(dto.getUsername());
        assertNull(dto.getIdRole());
        assertNull(dto.getRol());
    }
}
