package ec.edu.espe.asignaciones.entities;

import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class VehicleAssignmentTest {

    @Test
    void builderAndGetters() {
        UUID userId = UUID.randomUUID();
        UUID vehicleId = UUID.randomUUID();
        AssignmentId id = new AssignmentId(userId, vehicleId);

        VehicleAssignment a = VehicleAssignment.builder()
                .id(id)
                .active(true)
                .status(AssignmentStatus.ACTIVA)
                .assignmentType(AssignmentType.PROPIETARIO)
                .authorizationRoleId(UUID.randomUUID())
                .authorizationRoleName("CLIENTE")
                .validFrom(OffsetDateTime.now())
                .entryAuthorized(true)
                .observation("obs")
                .changeReason("reason")
                .build();

        assertEquals(id, a.getId());
        assertTrue(a.isActive());
        assertEquals(AssignmentStatus.ACTIVA, a.getStatus());
        assertEquals(AssignmentType.PROPIETARIO, a.getAssignmentType());
        assertEquals("CLIENTE", a.getAuthorizationRoleName());
        assertTrue(a.isEntryAuthorized());
        assertEquals("obs", a.getObservation());
        assertEquals("reason", a.getChangeReason());
    }

    @Test
    void onCreateSetsTimestamps() {
        VehicleAssignment a = new VehicleAssignment();
        a.setId(new AssignmentId(UUID.randomUUID(), UUID.randomUUID()));
        a.setActive(true);
        a.setStatus(AssignmentStatus.ACTIVA);
        a.setAssignmentType(AssignmentType.PROPIETARIO);
        a.setAuthorizationRoleId(UUID.randomUUID());
        a.setAuthorizationRoleName("CLIENTE");
        a.setEntryAuthorized(true);

        // Call lifecycle method directly
        a.onCreate();

        assertNotNull(a.getAssignedAt());
        assertNotNull(a.getUpdatedAt());
        assertNotNull(a.getValidFrom());
    }

    @Test
    void onCreateSetsStatusAndTypeDefaults() {
        VehicleAssignment a = new VehicleAssignment();
        // Don't set status, assignmentType - verify defaults are set
        a.onCreate();

        assertEquals(AssignmentStatus.ACTIVA, a.getStatus());
        assertEquals(AssignmentType.PROPIETARIO, a.getAssignmentType());
        assertTrue(a.isEntryAuthorized()); // ACTIVA -> entryAuthorized=true
    }

    @Test
    void onCreateSuspendedStatusDisablesEntryAuthorized() {
        VehicleAssignment a = new VehicleAssignment();
        a.setStatus(AssignmentStatus.SUSPENDIDA);
        a.onCreate();

        assertFalse(a.isEntryAuthorized());
    }

    @Test
    void onCreateUsesExistingValidFrom() {
        VehicleAssignment a = new VehicleAssignment();
        OffsetDateTime existing = OffsetDateTime.now().minusDays(5);
        a.setValidFrom(existing);
        a.onCreate();

        assertEquals(existing, a.getValidFrom());
    }

    @Test
    void onUpdateSetsUpdatedAt() {
        VehicleAssignment a = new VehicleAssignment();
        a.onUpdate();
        assertNotNull(a.getUpdatedAt());
    }

    @Test
    void noArgsConstructor() {
        VehicleAssignment a = new VehicleAssignment();
        assertNotNull(a);
    }
}
