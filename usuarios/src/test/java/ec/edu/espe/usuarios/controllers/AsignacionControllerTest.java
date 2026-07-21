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

import ec.edu.espe.usuarios.dtos.AsignacionResponseDto;
import ec.edu.espe.usuarios.dtos.AsignarRolRequestDto;
import ec.edu.espe.usuarios.services.AsignacionServicio;

class AsignacionControllerTest {

    private AsignacionServicio asignacionServicio;
    private AsignacionController controller;

    @BeforeEach
    void setUp() {
        asignacionServicio = mock(AsignacionServicio.class);
        controller = new AsignacionController(asignacionServicio);
    }

    private AsignacionResponseDto dto(UUID idUser, UUID idRole, boolean active) {
        return AsignacionResponseDto.builder().idUser(idUser).idRole(idRole).active(active).build();
    }

    @Test
    @DisplayName("listarAsignaciones retorna 200")
    void listarAsignaciones() {
        when(asignacionServicio.listarAsignaciones()).thenReturn(List.of(
                dto(UUID.randomUUID(), UUID.randomUUID(), true)));

        ResponseEntity<List<AsignacionResponseDto>> resp = controller.listarAsignaciones();

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertEquals(1, resp.getBody().size());
    }

    @Test
    @DisplayName("asignarRol retorna 201")
    void asignarRol() {
        UUID idUser = UUID.randomUUID();
        UUID idRole = UUID.randomUUID();
        AsignarRolRequestDto req = AsignarRolRequestDto.builder().idUser(idUser).idRole(idRole).build();
        when(asignacionServicio.asignarRol(req)).thenReturn(dto(idUser, idRole, true));

        ResponseEntity<AsignacionResponseDto> resp = controller.asignarRol(req);

        assertEquals(HttpStatus.CREATED, resp.getStatusCode());
        assertTrue(resp.getBody().isActive());
    }

    @Test
    @DisplayName("listarRolesDeUsuario retorna 200")
    void listarRolesDeUsuario() {
        UUID idUser = UUID.randomUUID();
        when(asignacionServicio.listarRolesDeUsuario(idUser))
                .thenReturn(List.of(dto(idUser, UUID.randomUUID(), true)));

        ResponseEntity<List<AsignacionResponseDto>> resp = controller.listarRolesDeUsuario(idUser);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertEquals(1, resp.getBody().size());
    }

    @Test
    @DisplayName("desactivarAsignacion retorna 200 con active=false")
    void desactivarAsignacion() {
        UUID idUser = UUID.randomUUID();
        UUID idRole = UUID.randomUUID();
        when(asignacionServicio.desactivarAsignacion(idUser, idRole))
                .thenReturn(dto(idUser, idRole, false));

        ResponseEntity<AsignacionResponseDto> resp = controller.desactivarAsignacion(idUser, idRole);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertFalse(resp.getBody().isActive());
    }

    @Test
    @DisplayName("activarAsignacion retorna 200 con active=true")
    void activarAsignacion() {
        UUID idUser = UUID.randomUUID();
        UUID idRole = UUID.randomUUID();
        when(asignacionServicio.activarAsignacion(idUser, idRole))
                .thenReturn(dto(idUser, idRole, true));

        ResponseEntity<AsignacionResponseDto> resp = controller.activarAsignacion(idUser, idRole);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertTrue(resp.getBody().isActive());
    }
}
