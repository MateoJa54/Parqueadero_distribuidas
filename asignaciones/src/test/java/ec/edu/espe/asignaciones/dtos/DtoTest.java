package ec.edu.espe.asignaciones.dtos;

import ec.edu.espe.asignaciones.entities.AuditAction;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class DtoTest {

    @Test
    void createAssignmentRequestGettersSetters() {
        CreateAssignmentRequest req = new CreateAssignmentRequest();
        UUID userId = UUID.randomUUID();
        UUID vehicleId = UUID.randomUUID();
        req.setUserId(userId);
        req.setVehicleId(vehicleId);
        req.setVehicleAlias("alias");
        req.setObservation("obs");

        assertEquals(userId, req.getUserId());
        assertEquals(vehicleId, req.getVehicleId());
        assertEquals("alias", req.getVehicleAlias());
        assertEquals("obs", req.getObservation());
    }

    @Test
    void updateAssignmentRequestGettersSetters() {
        UpdateAssignmentRequest req = new UpdateAssignmentRequest();
        req.setVehicleAlias("alias");
        req.setObservation("obs");
        req.setChangeReason("reason");
        req.setEntryAuthorized(true);
        OffsetDateTime dt = OffsetDateTime.now();
        req.setValidUntil(dt);

        assertEquals("alias", req.getVehicleAlias());
        assertEquals("obs", req.getObservation());
        assertEquals("reason", req.getChangeReason());
        assertTrue(req.getEntryAuthorized());
        assertEquals(dt, req.getValidUntil());
    }

    @Test
    void usuarioClientResponseGettersSetters() {
        UsuarioClientResponse resp = new UsuarioClientResponse();
        UUID id = UUID.randomUUID();
        resp.setId(id);
        resp.setUsername("user1");
        resp.setNombreCompleto("User One");
        resp.setActive(true);

        assertEquals(id, resp.getId());
        assertEquals("user1", resp.getUsername());
        assertEquals("User One", resp.getNombreCompleto());
        assertTrue(resp.isActive());
    }

    @Test
    void vehiculoClientResponseGettersSetters() {
        VehiculoClientResponse resp = new VehiculoClientResponse();
        UUID id = UUID.randomUUID();
        resp.setId(id);
        resp.setPlaca("ABC-1234");
        resp.setMarca("Toyota");
        resp.setModelo("Corolla");
        resp.setColor("Red");
        resp.setAnio(2020);
        resp.setClasificacion("auto");
        resp.setTipo("sedan");
        resp.setActivo(true);

        assertEquals(id, resp.getId());
        assertEquals("ABC-1234", resp.getPlaca());
        assertEquals("Toyota", resp.getMarca());
        assertEquals("Corolla", resp.getModelo());
        assertEquals("Red", resp.getColor());
        assertEquals(2020, resp.getAnio());
        assertEquals("auto", resp.getClasificacion());
        assertEquals("sedan", resp.getTipo());
        assertTrue(resp.isActivo());
    }

    @Test
    void userRoleAssignmentResponseGettersSetters() {
        UserRoleAssignmentResponse resp = new UserRoleAssignmentResponse();
        UUID id = UUID.randomUUID();
        resp.setIdRole(id);
        resp.setRol("ADMIN");
        resp.setActive(true);

        assertEquals(id, resp.getIdRole());
        assertEquals("ADMIN", resp.getRol());
        assertTrue(resp.isActive());
    }

    @Test
    void auditEventResponseBuilder() {
        UUID id = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID vehicleId = UUID.randomUUID();
        OffsetDateTime ts = OffsetDateTime.now();

        AuditEventResponse resp = AuditEventResponse.builder()
                .id(id)
                .userId(userId)
                .vehicleId(vehicleId)
                .action(AuditAction.CREACION)
                .timestamp(ts)
                .oldPayload("{}")
                .newPayload("{\"x\":1}")
                .build();

        assertEquals(id, resp.getId());
        assertEquals(userId, resp.getUserId());
        assertEquals(vehicleId, resp.getVehicleId());
        assertEquals(AuditAction.CREACION, resp.getAction());
        assertEquals(ts, resp.getTimestamp());
    }

    @Test
    void fleetVehicleResponseBuilder() {
        UUID userId = UUID.randomUUID();
        UUID vehicleId = UUID.randomUUID();
        FleetVehicleResponse resp = FleetVehicleResponse.builder()
                .userId(userId)
                .vehicleId(vehicleId)
                .placa("XYZ-999")
                .marca("Honda")
                .modelo("Civic")
                .color("Blue")
                .anio(2023)
                .tipo("sedan")
                .clasificacion("auto")
                .entryAuthorized(true)
                .build();

        assertEquals(userId, resp.getUserId());
        assertEquals(vehicleId, resp.getVehicleId());
        assertEquals("XYZ-999", resp.getPlaca());
        assertTrue(resp.isEntryAuthorized());
    }
}
