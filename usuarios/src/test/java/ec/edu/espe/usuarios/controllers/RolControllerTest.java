package ec.edu.espe.usuarios.controllers;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import ec.edu.espe.usuarios.dtos.RolRequestDto;
import ec.edu.espe.usuarios.dtos.RolResponseDto;
import ec.edu.espe.usuarios.services.RolServicio;

class RolControllerTest {

    private RolServicio rolServicio;
    private RolController controller;

    @BeforeEach
    void setUp() {
        rolServicio = mock(RolServicio.class);
        controller = new RolController(rolServicio);
    }

    private RolResponseDto dto(UUID id, String name) {
        return RolResponseDto.builder().id(id).name(name).active(true).build();
    }

    @Test
    @DisplayName("listarRoles retorna 200")
    void listarRoles() {
        when(rolServicio.listarRoles()).thenReturn(List.of(dto(UUID.randomUUID(), "ADMIN")));

        ResponseEntity<List<RolResponseDto>> resp = controller.listarRoles();

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertEquals(1, resp.getBody().size());
    }

    @Test
    @DisplayName("crearRol retorna 201")
    void crearRol() {
        UUID id = UUID.randomUUID();
        RolRequestDto req = RolRequestDto.builder().name("ADMIN").description("desc").build();
        when(rolServicio.crearRol(req)).thenReturn(dto(id, "ADMIN"));

        ResponseEntity<RolResponseDto> resp = controller.crearRol(req);

        assertEquals(HttpStatus.CREATED, resp.getStatusCode());
        assertEquals(id, resp.getBody().getId());
    }

    @Test
    @DisplayName("obtenerRol retorna 200")
    void obtenerRol() {
        UUID id = UUID.randomUUID();
        when(rolServicio.obtenerRol(id)).thenReturn(dto(id, "ADMIN"));

        ResponseEntity<RolResponseDto> resp = controller.obtenerRol(id);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
    }

    @Test
    @DisplayName("actualizarRol retorna 200")
    void actualizarRol() {
        UUID id = UUID.randomUUID();
        RolRequestDto req = RolRequestDto.builder().name("NUEVO").build();
        when(rolServicio.actualizarRol(id, req)).thenReturn(dto(id, "NUEVO"));

        ResponseEntity<RolResponseDto> resp = controller.actualizarRol(id, req);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
    }

    @Test
    @DisplayName("activarRol retorna 200")
    void activarRol() {
        UUID id = UUID.randomUUID();
        when(rolServicio.activarRol(id)).thenReturn(dto(id, "ADMIN"));

        ResponseEntity<RolResponseDto> resp = controller.activarRol(id);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
    }

    @Test
    @DisplayName("desactivarRol retorna 200")
    void desactivarRol() {
        UUID id = UUID.randomUUID();
        RolResponseDto inactivo = RolResponseDto.builder().id(id).active(false).build();
        when(rolServicio.desactivarRol(id)).thenReturn(inactivo);

        ResponseEntity<RolResponseDto> resp = controller.desactivarRol(id);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertFalse(resp.getBody().isActive());
    }
}
