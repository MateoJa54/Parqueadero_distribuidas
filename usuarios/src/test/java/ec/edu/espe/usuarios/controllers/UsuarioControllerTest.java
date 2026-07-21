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

import ec.edu.espe.usuarios.dtos.UsuarioRequestDto;
import ec.edu.espe.usuarios.dtos.UsuarioResponseDto;
import ec.edu.espe.usuarios.dtos.UsuarioUpdateDto;
import ec.edu.espe.usuarios.services.UsuarioServicio;

class UsuarioControllerTest {

    private UsuarioServicio usuarioServicio;
    private UsuarioController controller;

    @BeforeEach
    void setUp() {
        usuarioServicio = mock(UsuarioServicio.class);
        controller = new UsuarioController(usuarioServicio);
    }

    private UsuarioResponseDto dto(UUID id, String username, boolean active) {
        return UsuarioResponseDto.builder().id(id).username(username).active(active).build();
    }

    @Test
    @DisplayName("listarUsuarios retorna 200")
    void listarUsuarios() {
        UUID id = UUID.randomUUID();
        when(usuarioServicio.listarUsuarios()).thenReturn(List.of(dto(id, "user1", true)));

        ResponseEntity<List<UsuarioResponseDto>> resp = controller.listarUsuarios();

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertEquals(1, resp.getBody().size());
    }

    @Test
    @DisplayName("obtenerUsuario retorna 200")
    void obtenerUsuario() {
        UUID id = UUID.randomUUID();
        when(usuarioServicio.obtenerUsuario(id)).thenReturn(dto(id, "user1", true));

        ResponseEntity<UsuarioResponseDto> resp = controller.obtenerUsuario(id);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertEquals(id, resp.getBody().getId());
    }

    @Test
    @DisplayName("buscarUsuarios retorna 200")
    void buscarUsuarios() {
        UUID id = UUID.randomUUID();
        when(usuarioServicio.buscarUsuarios("user")).thenReturn(List.of(dto(id, "user1", true)));

        ResponseEntity<List<UsuarioResponseDto>> resp = controller.buscarUsuarios("user");

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertEquals(1, resp.getBody().size());
    }

    @Test
    @DisplayName("crearUsuario retorna 201")
    void crearUsuario() {
        UUID id = UUID.randomUUID();
        UsuarioRequestDto req = UsuarioRequestDto.builder()
                .idPersona(id).username("user1").password("Pass1word").build();
        when(usuarioServicio.crearUsuario(req)).thenReturn(dto(id, "user1", true));

        ResponseEntity<UsuarioResponseDto> resp = controller.crearUsuario(req);

        assertEquals(HttpStatus.CREATED, resp.getStatusCode());
    }

    @Test
    @DisplayName("actualizarUsuario retorna 200")
    void actualizarUsuario() {
        UUID id = UUID.randomUUID();
        UsuarioUpdateDto req = UsuarioUpdateDto.builder()
                .idPersona(id).username("user1").build();
        when(usuarioServicio.actualizarUsuario(id, req)).thenReturn(dto(id, "user1", true));

        ResponseEntity<UsuarioResponseDto> resp = controller.actualizarUsuario(id, req);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
    }

    @Test
    @DisplayName("activarUsuario retorna 200")
    void activarUsuario() {
        UUID id = UUID.randomUUID();
        when(usuarioServicio.activarUsuario(id)).thenReturn(dto(id, "user1", true));

        ResponseEntity<UsuarioResponseDto> resp = controller.activarUsuario(id);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertTrue(resp.getBody().isActive());
    }

    @Test
    @DisplayName("desactivarUsuario retorna 200")
    void desactivarUsuario() {
        UUID id = UUID.randomUUID();
        when(usuarioServicio.desactivarUsuario(id)).thenReturn(dto(id, "user1", false));

        ResponseEntity<UsuarioResponseDto> resp = controller.desactivarUsuario(id);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertFalse(resp.getBody().isActive());
    }
}
