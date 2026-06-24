package ec.edu.espe.asignaciones.services;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import com.fasterxml.jackson.databind.ObjectMapper;

import ec.edu.espe.asignaciones.dtos.UsuarioClientResponse;
import ec.edu.espe.asignaciones.dtos.CreateAssignmentRequest;
import ec.edu.espe.asignaciones.dtos.UserRoleAssignmentResponse;
import ec.edu.espe.asignaciones.dtos.VehiculoClientResponse;
import ec.edu.espe.asignaciones.entities.AssignmentId;
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
        UUID roleId = UUID.randomUUID();
        UUID otherUserId = UUID.randomUUID();
        CreateAssignmentRequest request = new CreateAssignmentRequest();
        request.setUserId(userId);
        request.setVehicleId(vehicleId);
        request.setRoleId(roleId);

        when(externalCatalogService.validarUsuarioActivo(userId)).thenReturn(new UsuarioClientResponse());
        when(externalCatalogService.validarVehiculoActivo(vehicleId)).thenReturn(new VehiculoClientResponse());
        when(externalCatalogService.validarRolAutorizadoParaAsignacion(userId, roleId))
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
}
