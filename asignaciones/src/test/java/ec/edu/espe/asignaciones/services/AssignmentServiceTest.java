package ec.edu.espe.asignaciones.services;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import org.springframework.context.ApplicationEventPublisher;

import com.fasterxml.jackson.databind.ObjectMapper;

import ec.edu.espe.asignaciones.dtos.CreateAssignmentRequest;
import ec.edu.espe.asignaciones.dtos.UpdateAssignmentRequest;
import ec.edu.espe.asignaciones.dtos.UserRoleAssignmentResponse;
import ec.edu.espe.asignaciones.dtos.UsuarioClientResponse;
import ec.edu.espe.asignaciones.dtos.VehiculoClientResponse;
import ec.edu.espe.asignaciones.entities.AssignmentId;
import ec.edu.espe.asignaciones.entities.AssignmentStatus;
import ec.edu.espe.asignaciones.entities.VehicleAssignment;
import ec.edu.espe.asignaciones.repositories.AssignmentAuditEventRepository;
import ec.edu.espe.asignaciones.repositories.VehicleAssignmentRepository;
import ec.edu.espe.asignaciones.utils.ReglaNegocioException;

class AssignmentServiceTest {

    private VehicleAssignmentRepository assignmentRepository;
    private AssignmentAuditEventRepository auditRepository;
    private ExternalCatalogService externalCatalogService;
    private ApplicationEventPublisher eventPublisher;
    private AssignmentService assignmentService;

    @BeforeEach
    void setUp() {
        assignmentRepository = mock(VehicleAssignmentRepository.class);
        auditRepository = mock(AssignmentAuditEventRepository.class);
        externalCatalogService = mock(ExternalCatalogService.class);
        eventPublisher = mock(ApplicationEventPublisher.class);
        assignmentService = new AssignmentService(
                assignmentRepository,
                auditRepository,
                externalCatalogService,
                eventPublisher,
                new ObjectMapper());
    }

    @Test
    void crearAsignacionRechazaVehiculoConPropietarioActivo() {
        UUID userId = UUID.randomUUID();
        UUID vehicleId = UUID.randomUUID();
        UUID otherUserId = UUID.randomUUID();
        CreateAssignmentRequest request = new CreateAssignmentRequest();
        request.setUserId(userId);
        request.setVehicleId(vehicleId);

        when(externalCatalogService.validarUsuarioActivo(userId)).thenReturn(new UsuarioClientResponse());
        when(externalCatalogService.validarVehiculoActivo(vehicleId)).thenReturn(new VehiculoClientResponse());
        when(externalCatalogService.validarRolAutorizadoParaAsignacion(userId))
                .thenReturn(new UserRoleAssignmentResponse());
        when(assignmentRepository.findByIdVehicleIdAndActiveTrue(vehicleId))
                .thenReturn(Optional.of(VehicleAssignment.builder()
                        .id(new AssignmentId(otherUserId, vehicleId))
                        .active(true)
                        .build()));

        assertThrows(ReglaNegocioException.class,
                () -> assignmentService.crearAsignacion(request));

        verifyNoInteractions(eventPublisher);
        verifyNoInteractions(auditRepository);
    }

    @Test
    void reactivarAsignacionRechazaVehiculoConOtroPropietarioActivo() {
        UUID userId = UUID.randomUUID();
        UUID vehicleId = UUID.randomUUID();
        UUID otherUserId = UUID.randomUUID();

        when(externalCatalogService.validarUsuarioActivo(userId)).thenReturn(new UsuarioClientResponse());
        when(externalCatalogService.validarVehiculoActivo(vehicleId)).thenReturn(new VehiculoClientResponse());
        when(assignmentRepository.findByIdVehicleIdAndActiveTrue(vehicleId))
                .thenReturn(Optional.of(VehicleAssignment.builder()
                        .id(new AssignmentId(otherUserId, vehicleId))
                        .active(true)
                        .build()));

        assertThrows(ReglaNegocioException.class,
                () -> assignmentService.reactivarAsignacion(userId, vehicleId));

        verifyNoInteractions(eventPublisher);
        verifyNoInteractions(auditRepository);
    }

    @Test
    void modificarAsignacionRevalidaRolAlReactivarDesdeSuspendida() {
        UUID userId = UUID.randomUUID();
        UUID vehicleId = UUID.randomUUID();
        UUID roleId = UUID.randomUUID();

        VehicleAssignment suspendida = VehicleAssignment.builder()
                .id(new AssignmentId(userId, vehicleId))
                .active(true)
                .status(AssignmentStatus.SUSPENDIDA)
                .authorizationRoleId(roleId)
                .build();
        when(assignmentRepository.findById(new AssignmentId(userId, vehicleId)))
                .thenReturn(Optional.of(suspendida));
        when(externalCatalogService.validarUsuarioActivo(userId)).thenReturn(new UsuarioClientResponse());
        when(externalCatalogService.validarRolAutorizadoParaAsignacion(userId))
                .thenThrow(new ReglaNegocioException("El usuario no tiene un rol activo"));

        UpdateAssignmentRequest request = new UpdateAssignmentRequest();
        request.setStatus(AssignmentStatus.ACTIVA);

        assertThrows(ReglaNegocioException.class,
                () -> assignmentService.modificarAsignacion(userId, vehicleId, request));

        verifyNoInteractions(eventPublisher);
        verifyNoInteractions(auditRepository);
    }
}
