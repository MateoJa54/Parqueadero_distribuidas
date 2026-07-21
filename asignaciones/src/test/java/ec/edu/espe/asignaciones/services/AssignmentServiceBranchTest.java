package ec.edu.espe.asignaciones.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import ec.edu.espe.asignaciones.dtos.AssignmentResponse;
import ec.edu.espe.asignaciones.dtos.UpdateAssignmentRequest;
import ec.edu.espe.asignaciones.dtos.UserRoleAssignmentResponse;
import ec.edu.espe.asignaciones.dtos.UsuarioClientResponse;
import ec.edu.espe.asignaciones.dtos.VehiculoClientResponse;
import ec.edu.espe.asignaciones.entities.AssignmentId;
import ec.edu.espe.asignaciones.entities.AssignmentStatus;
import ec.edu.espe.asignaciones.entities.AssignmentType;
import ec.edu.espe.asignaciones.entities.VehicleAssignment;
import ec.edu.espe.asignaciones.repositories.AssignmentAuditEventRepository;
import ec.edu.espe.asignaciones.repositories.VehicleAssignmentRepository;
import ec.edu.espe.asignaciones.utils.ReglaNegocioException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Additional branch coverage for AssignmentService: aplicarCambioEstado,
 * modificarAsignacion with status/entryAuthorized branches, crearAsignacion
 * with vehicle already having active owner, and reactivarAsignacion when
 * vehicle already has active owner.
 */
class AssignmentServiceBranchTest {

    private VehicleAssignmentRepository assignmentRepository;
    private AssignmentAuditEventRepository auditRepository;
    private ExternalCatalogService externalCatalogService;
    private ApplicationEventPublisher eventPublisher;
    private AssignmentService service;

    @BeforeEach
    void setUp() {
        assignmentRepository = mock(VehicleAssignmentRepository.class);
        auditRepository = mock(AssignmentAuditEventRepository.class);
        externalCatalogService = mock(ExternalCatalogService.class);
        eventPublisher = mock(ApplicationEventPublisher.class);
        service = new AssignmentService(
                assignmentRepository,
                auditRepository,
                externalCatalogService,
                eventPublisher,
                new ObjectMapper().registerModule(new JavaTimeModule()));
    }

    private VehicleAssignment activeAssignment(UUID userId, UUID vehicleId) {
        return VehicleAssignment.builder()
                .id(new AssignmentId(userId, vehicleId))
                .active(true)
                .status(AssignmentStatus.ACTIVA)
                .assignmentType(AssignmentType.PROPIETARIO)
                .authorizationRoleId(UUID.randomUUID())
                .authorizationRoleName("CLIENTE")
                .entryAuthorized(true)
                .validFrom(OffsetDateTime.now().minusDays(1))
                .build();
    }

    private UserRoleAssignmentResponse roleResponse() {
        UserRoleAssignmentResponse r = new UserRoleAssignmentResponse();
        r.setIdRole(UUID.randomUUID());
        r.setRol("CLIENTE");
        r.setActive(true);
        return r;
    }

    // --- crearAsignacion ---

    @Test
    @DisplayName("crearAsignacion rechaza si otro vehiculo ya tiene propietario activo")
    void crearAsignacionVehiculoYaTieneActivoPropietario() {
        UUID userId = UUID.randomUUID();
        UUID vehicleId = UUID.randomUUID();
        var request = new ec.edu.espe.asignaciones.dtos.CreateAssignmentRequest();
        request.setUserId(userId);
        request.setVehicleId(vehicleId);

        VehicleAssignment otherOwner = activeAssignment(UUID.randomUUID(), vehicleId);
        when(externalCatalogService.validarUsuarioActivo(userId, null)).thenReturn(new UsuarioClientResponse());
        when(externalCatalogService.validarVehiculoActivo(vehicleId, null)).thenReturn(new VehiculoClientResponse());
        when(externalCatalogService.validarRolAutorizadoParaAsignacion(userId, null)).thenReturn(roleResponse());
        when(assignmentRepository.findByIdVehicleIdAndActiveTrue(vehicleId)).thenReturn(Optional.of(otherOwner));

        assertThrows(ReglaNegocioException.class, () -> service.crearAsignacion(request, null));
    }

    // --- modificarAsignacion: status changes ---

    @Test
    @DisplayName("modificarAsignacion cambia status a SUSPENDIDA desautoriza ingreso")
    void modificarAsignacionStatusSuspendida() {
        UUID userId = UUID.randomUUID();
        UUID vehicleId = UUID.randomUUID();
        VehicleAssignment a = activeAssignment(userId, vehicleId);

        UpdateAssignmentRequest request = new UpdateAssignmentRequest();
        request.setStatus(AssignmentStatus.SUSPENDIDA);

        when(assignmentRepository.findById(new AssignmentId(userId, vehicleId))).thenReturn(Optional.of(a));
        when(assignmentRepository.save(any())).thenReturn(a);

        AssignmentResponse response = service.modificarAsignacion(userId, vehicleId, request, null);

        assertNotNull(response);
        assertFalse(a.isEntryAuthorized());
        assertEquals(AssignmentStatus.SUSPENDIDA, a.getStatus());
        verify(eventPublisher).publishEvent(any(Object.class));
    }

    @Test
    @DisplayName("modificarAsignacion cambia status a FINALIZADA desactiva asignacion")
    void modificarAsignacionStatusFinalizada() {
        UUID userId = UUID.randomUUID();
        UUID vehicleId = UUID.randomUUID();
        VehicleAssignment a = activeAssignment(userId, vehicleId);

        UpdateAssignmentRequest request = new UpdateAssignmentRequest();
        request.setStatus(AssignmentStatus.FINALIZADA);

        when(assignmentRepository.findById(new AssignmentId(userId, vehicleId))).thenReturn(Optional.of(a));
        when(assignmentRepository.save(any())).thenReturn(a);

        service.modificarAsignacion(userId, vehicleId, request, null);

        assertFalse(a.isActive());
        assertEquals(AssignmentStatus.FINALIZADA, a.getStatus());
    }

    @Test
    @DisplayName("modificarAsignacion cambia status de SUSPENDIDA a ACTIVA revalida usuario y rol")
    void modificarAsignacionStatusSuspendidaAActiva() {
        UUID userId = UUID.randomUUID();
        UUID vehicleId = UUID.randomUUID();
        VehicleAssignment a = VehicleAssignment.builder()
                .id(new AssignmentId(userId, vehicleId))
                .active(true)
                .status(AssignmentStatus.SUSPENDIDA)
                .assignmentType(AssignmentType.PROPIETARIO)
                .authorizationRoleId(UUID.randomUUID())
                .authorizationRoleName("CLIENTE")
                .entryAuthorized(false)
                .validFrom(OffsetDateTime.now().minusDays(1))
                .build();

        UpdateAssignmentRequest request = new UpdateAssignmentRequest();
        request.setStatus(AssignmentStatus.ACTIVA);

        when(assignmentRepository.findById(new AssignmentId(userId, vehicleId))).thenReturn(Optional.of(a));
        when(externalCatalogService.validarUsuarioActivo(userId, null)).thenReturn(new UsuarioClientResponse());
        when(externalCatalogService.validarRolAutorizadoParaAsignacion(userId, null)).thenReturn(roleResponse());
        when(assignmentRepository.save(any())).thenReturn(a);

        service.modificarAsignacion(userId, vehicleId, request, null);

        assertEquals(AssignmentStatus.ACTIVA, a.getStatus());
        assertTrue(a.isEntryAuthorized());
        verify(externalCatalogService).validarUsuarioActivo(userId, null);
        verify(externalCatalogService).validarRolAutorizadoParaAsignacion(userId, null);
    }

    @Test
    @DisplayName("modificarAsignacion establece entryAuthorized false cuando status no ACTIVA")
    void modificarAsignacionEntryAuthorizedFalseNoActiva() {
        UUID userId = UUID.randomUUID();
        UUID vehicleId = UUID.randomUUID();
        VehicleAssignment a = activeAssignment(userId, vehicleId);

        UpdateAssignmentRequest request = new UpdateAssignmentRequest();
        request.setEntryAuthorized(false);

        when(assignmentRepository.findById(new AssignmentId(userId, vehicleId))).thenReturn(Optional.of(a));
        when(assignmentRepository.save(any())).thenReturn(a);

        service.modificarAsignacion(userId, vehicleId, request, null);

        assertFalse(a.isEntryAuthorized());
    }

    @Test
    @DisplayName("modificarAsignacion cambia assignmentType")
    void modificarAsignacionTipo() {
        UUID userId = UUID.randomUUID();
        UUID vehicleId = UUID.randomUUID();
        VehicleAssignment a = activeAssignment(userId, vehicleId);

        UpdateAssignmentRequest request = new UpdateAssignmentRequest();
        request.setAssignmentType(AssignmentType.AUTORIZADO);

        when(assignmentRepository.findById(new AssignmentId(userId, vehicleId))).thenReturn(Optional.of(a));
        when(assignmentRepository.save(any())).thenReturn(a);

        service.modificarAsignacion(userId, vehicleId, request, null);

        assertEquals(AssignmentType.AUTORIZADO, a.getAssignmentType());
    }

    @Test
    @DisplayName("modificarAsignacion actualiza validUntil valido")
    void modificarAsignacionValidUntilValido() {
        UUID userId = UUID.randomUUID();
        UUID vehicleId = UUID.randomUUID();
        VehicleAssignment a = activeAssignment(userId, vehicleId);

        UpdateAssignmentRequest request = new UpdateAssignmentRequest();
        request.setValidUntil(OffsetDateTime.now().plusDays(30));

        when(assignmentRepository.findById(new AssignmentId(userId, vehicleId))).thenReturn(Optional.of(a));
        when(assignmentRepository.save(any())).thenReturn(a);

        service.modificarAsignacion(userId, vehicleId, request, null);

        assertNotNull(a.getValidUntil());
    }

    @Test
    @DisplayName("modificarAsignacion actualiza changeReason")
    void modificarAsignacionChangeReason() {
        UUID userId = UUID.randomUUID();
        UUID vehicleId = UUID.randomUUID();
        VehicleAssignment a = activeAssignment(userId, vehicleId);

        UpdateAssignmentRequest request = new UpdateAssignmentRequest();
        request.setChangeReason("porque si");

        when(assignmentRepository.findById(new AssignmentId(userId, vehicleId))).thenReturn(Optional.of(a));
        when(assignmentRepository.save(any())).thenReturn(a);

        service.modificarAsignacion(userId, vehicleId, request, null);

        assertEquals("porque si", a.getChangeReason());
    }

    // --- reactivarAsignacion: vehicle already has active owner ---

    @Test
    @DisplayName("reactivarAsignacion rechaza si vehiculo ya tiene propietario activo")
    void reactivarAsignacionVehiculoYaActivo() {
        UUID userId = UUID.randomUUID();
        UUID vehicleId = UUID.randomUUID();
        VehicleAssignment owner = activeAssignment(UUID.randomUUID(), vehicleId);

        when(externalCatalogService.validarUsuarioActivo(userId, null)).thenReturn(new UsuarioClientResponse());
        when(externalCatalogService.validarVehiculoActivo(vehicleId, null)).thenReturn(new VehiculoClientResponse());
        when(assignmentRepository.findByIdVehicleIdAndActiveTrue(vehicleId)).thenReturn(Optional.of(owner));

        assertThrows(ReglaNegocioException.class, () -> service.reactivarAsignacion(userId, vehicleId, null));
    }
}
