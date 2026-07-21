package ec.edu.espe.asignaciones.controllers;

import ec.edu.espe.asignaciones.dtos.AssignmentResponse;
import ec.edu.espe.asignaciones.dtos.AuditEventResponse;
import ec.edu.espe.asignaciones.dtos.CreateAssignmentRequest;
import ec.edu.espe.asignaciones.dtos.UpdateAssignmentRequest;
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

class AssignmentControllerTest {

    private AssignmentService assignmentService;
    private AssignmentController controller;

    @BeforeEach
    void setUp() {
        assignmentService = mock(AssignmentService.class);
        controller = new AssignmentController(assignmentService);
    }

    private AssignmentResponse sampleResponse(UUID userId, UUID vehicleId) {
        return AssignmentResponse.builder()
                .userId(userId)
                .vehicleId(vehicleId)
                .active(true)
                .status(AssignmentStatus.ACTIVA)
                .assignmentType(AssignmentType.PROPIETARIO)
                .entryAuthorized(true)
                .build();
    }

    @Test
    @DisplayName("listarAsignaciones devuelve 200 con lista")
    void listarAsignaciones() {
        UUID u = UUID.randomUUID();
        UUID v = UUID.randomUUID();
        when(assignmentService.listarAsignaciones(false)).thenReturn(List.of(sampleResponse(u, v)));

        ResponseEntity<List<AssignmentResponse>> response = controller.listarAsignaciones(false);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
    }

    @Test
    @DisplayName("listarAsignaciones con soloActivas=true")
    void listarAsignacionesSoloActivas() {
        when(assignmentService.listarAsignaciones(true)).thenReturn(List.of());

        ResponseEntity<List<AssignmentResponse>> response = controller.listarAsignaciones(true);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isEmpty());
    }

    @Test
    @DisplayName("crearAsignacion devuelve 201 CREATED")
    void crearAsignacion() {
        UUID u = UUID.randomUUID();
        UUID v = UUID.randomUUID();
        CreateAssignmentRequest request = new CreateAssignmentRequest();
        request.setUserId(u);
        request.setVehicleId(v);

        when(assignmentService.crearAsignacion(any(), any())).thenReturn(sampleResponse(u, v));

        ResponseEntity<AssignmentResponse> response = controller.crearAsignacion(request, "Bearer token");

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(u, response.getBody().getUserId());
    }

    @Test
    @DisplayName("modificarAsignacion devuelve 200 OK")
    void modificarAsignacion() {
        UUID u = UUID.randomUUID();
        UUID v = UUID.randomUUID();
        UpdateAssignmentRequest request = new UpdateAssignmentRequest();
        request.setVehicleAlias("alias");

        when(assignmentService.modificarAsignacion(eq(u), eq(v), any(), any())).thenReturn(sampleResponse(u, v));

        ResponseEntity<AssignmentResponse> response = controller.modificarAsignacion(u, v, request, null);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    @DisplayName("desactivarAsignacion devuelve 200 OK")
    void desactivarAsignacion() {
        UUID u = UUID.randomUUID();
        UUID v = UUID.randomUUID();
        AssignmentResponse resp = AssignmentResponse.builder()
                .userId(u).vehicleId(v).active(false)
                .status(AssignmentStatus.FINALIZADA).entryAuthorized(false)
                .assignmentType(AssignmentType.PROPIETARIO).build();

        when(assignmentService.desactivarAsignacion(u, v)).thenReturn(resp);

        ResponseEntity<AssignmentResponse> response = controller.desactivarAsignacion(u, v);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertFalse(response.getBody().isActive());
    }

    @Test
    @DisplayName("reactivarAsignacion devuelve 200 OK")
    void reactivarAsignacion() {
        UUID u = UUID.randomUUID();
        UUID v = UUID.randomUUID();
        when(assignmentService.reactivarAsignacion(eq(u), eq(v), any())).thenReturn(sampleResponse(u, v));

        ResponseEntity<AssignmentResponse> response = controller.reactivarAsignacion(u, v, "Bearer token");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().isActive());
    }

    @Test
    @DisplayName("consultarAsignacionActivaPorVehiculo devuelve 200 OK")
    void consultarAsignacionActivaPorVehiculo() {
        UUID u = UUID.randomUUID();
        UUID v = UUID.randomUUID();
        when(assignmentService.consultarAsignacionActivaPorVehiculo(v)).thenReturn(sampleResponse(u, v));

        ResponseEntity<AssignmentResponse> response = controller.consultarAsignacionActivaPorVehiculo(v);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(v, response.getBody().getVehicleId());
    }

    @Test
    @DisplayName("consultarTrazabilidad devuelve 200 OK con lista de eventos")
    void consultarTrazabilidad() {
        UUID u = UUID.randomUUID();
        UUID v = UUID.randomUUID();
        AuditEventResponse event = AuditEventResponse.builder()
                .id(UUID.randomUUID())
                .userId(u)
                .vehicleId(v)
                .build();

        when(assignmentService.consultarTrazabilidad(u, v)).thenReturn(List.of(event));

        ResponseEntity<List<AuditEventResponse>> response = controller.consultarTrazabilidad(u, v);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
    }
}
