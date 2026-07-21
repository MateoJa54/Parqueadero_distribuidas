package ec.edu.espe.usuarios.controllers;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import ec.edu.espe.usuarios.dtos.PersonaRequestDto;
import ec.edu.espe.usuarios.dtos.PersonaResponseDto;
import ec.edu.espe.usuarios.services.PersonaServicio;

class PersonaControllerTest {

    private PersonaServicio personaServicio;
    private PersonaController controller;

    @BeforeEach
    void setUp() {
        personaServicio = mock(PersonaServicio.class);
        controller = new PersonaController(personaServicio);
    }

    private PersonaResponseDto dto(UUID id) {
        return PersonaResponseDto.builder().id(id).firstName("Juan").lastName("Perez").active(true).build();
    }

    @Test
    @DisplayName("listarPersonas retorna 200 con lista")
    void listarPersonas() {
        UUID id = UUID.randomUUID();
        when(personaServicio.listarPersonas()).thenReturn(List.of(dto(id)));

        ResponseEntity<List<PersonaResponseDto>> resp = controller.listarPersonas();

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertNotNull(resp.getBody());
        assertEquals(1, resp.getBody().size());
    }

    @Test
    @DisplayName("obtenerPersona retorna 200")
    void obtenerPersona() {
        UUID id = UUID.randomUUID();
        when(personaServicio.obtenerPersona(id)).thenReturn(dto(id));

        ResponseEntity<PersonaResponseDto> resp = controller.obtenerPersona(id);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertEquals(id, resp.getBody().getId());
    }

    @Test
    @DisplayName("buscarPersonas retorna 200")
    void buscarPersonas() {
        UUID id = UUID.randomUUID();
        when(personaServicio.buscarPersonas("1713175071", null, null)).thenReturn(List.of(dto(id)));

        ResponseEntity<List<PersonaResponseDto>> resp = controller.buscarPersonas("1713175071", null, null);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertEquals(1, resp.getBody().size());
    }

    @Test
    @DisplayName("crearPersona retorna 201 con el dto creado")
    void crearPersona() {
        UUID id = UUID.randomUUID();
        PersonaRequestDto req = PersonaRequestDto.builder()
                .firstName("Juan").lastName("Perez").dni("1713175071")
                .email("j@x.com").phone("0991234567").nationality("EC").build();
        when(personaServicio.crearPersona(req)).thenReturn(dto(id));

        ResponseEntity<PersonaResponseDto> resp = controller.crearPersona(req);

        assertEquals(HttpStatus.CREATED, resp.getStatusCode());
        assertEquals(id, resp.getBody().getId());
    }

    @Test
    @DisplayName("actualizarPersona retorna 200")
    void actualizarPersona() {
        UUID id = UUID.randomUUID();
        PersonaRequestDto req = PersonaRequestDto.builder()
                .firstName("Juan").lastName("Perez").dni("1713175071")
                .email("j@x.com").phone("0991234567").nationality("EC").build();
        when(personaServicio.actualizarPersona(id, req)).thenReturn(dto(id));

        ResponseEntity<PersonaResponseDto> resp = controller.actualizarPersona(id, req);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
    }

    @Test
    @DisplayName("activarPersona retorna 200")
    void activarPersona() {
        UUID id = UUID.randomUUID();
        when(personaServicio.activarPersona(id)).thenReturn(dto(id));

        ResponseEntity<PersonaResponseDto> resp = controller.activarPersona(id);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
    }

    @Test
    @DisplayName("desactivarPersona retorna 200")
    void desactivarPersona() {
        UUID id = UUID.randomUUID();
        PersonaResponseDto inactivo = PersonaResponseDto.builder().id(id).active(false).build();
        when(personaServicio.desactivarPersona(id)).thenReturn(inactivo);

        ResponseEntity<PersonaResponseDto> resp = controller.desactivarPersona(id);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertFalse(resp.getBody().isActive());
    }
}
