package ec.edu.espe.asignaciones.controllers;

import ec.edu.espe.asignaciones.dtos.FleetVehicleResponse;
import ec.edu.espe.asignaciones.entities.AssignmentStatus;
import ec.edu.espe.asignaciones.entities.AssignmentType;
import ec.edu.espe.asignaciones.services.AssignmentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class FleetControllerTest {

    private AssignmentService assignmentService;
    private FleetController controller;

    @BeforeEach
    void setUp() {
        assignmentService = mock(AssignmentService.class);
        controller = new FleetController(assignmentService);
    }

    @Test
    @DisplayName("consultarFlota devuelve 200 con lista de vehiculos")
    void consultarFlota() {
        UUID userId = UUID.randomUUID();
        UUID vehicleId = UUID.randomUUID();
        FleetVehicleResponse vehicle = FleetVehicleResponse.builder()
                .userId(userId)
                .vehicleId(vehicleId)
                .placa("ABC-1234")
                .marca("Toyota")
                .modelo("Corolla")
                .status(AssignmentStatus.ACTIVA)
                .assignmentType(AssignmentType.PROPIETARIO)
                .build();

        when(assignmentService.consultarFlota(eq(userId), any())).thenReturn(List.of(vehicle));

        ResponseEntity<List<FleetVehicleResponse>> response = controller.consultarFlota(userId, "Bearer token");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        assertEquals(vehicleId, response.getBody().get(0).getVehicleId());
    }

    @Test
    @DisplayName("consultarFlota devuelve lista vacia")
    void consultarFlotaVacia() {
        UUID userId = UUID.randomUUID();
        when(assignmentService.consultarFlota(eq(userId), any())).thenReturn(List.of());

        ResponseEntity<List<FleetVehicleResponse>> response = controller.consultarFlota(userId, null);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().isEmpty());
    }
}
