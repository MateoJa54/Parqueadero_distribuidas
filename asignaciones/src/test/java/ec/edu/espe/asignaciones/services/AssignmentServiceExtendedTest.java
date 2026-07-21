package ec.edu.espe.asignaciones.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import ec.edu.espe.asignaciones.dtos.AssignmentResponse;
import ec.edu.espe.asignaciones.dtos.AuditEventResponse;
import ec.edu.espe.asignaciones.dtos.CreateAssignmentRequest;
import ec.edu.espe.asignaciones.dtos.FleetVehicleResponse;
import ec.edu.espe.asignaciones.dtos.UpdateAssignmentRequest;
import ec.edu.espe.asignaciones.dtos.UserRoleAssignmentResponse;
import ec.edu.espe.asignaciones.dtos.UsuarioClientResponse;
import ec.edu.espe.asignaciones.dtos.VehiculoClientResponse;
import ec.edu.espe.asignaciones.entities.AssignmentAuditEvent;
import ec.edu.espe.asignaciones.entities.AssignmentId;
import ec.edu.espe.asignaciones.entities.AssignmentStatus;
import ec.edu.espe.asignaciones.entities.AssignmentType;
import ec.edu.espe.asignaciones.entities.VehicleAssignment;
import ec.edu.espe.asignaciones.repositories.AssignmentAuditEventRepository;
import ec.edu.espe.asignaciones.repositories.VehicleAssignmentRepository;
import ec.edu.espe.asignaciones.utils.RecursoNoEncontradoException;
import ec.edu.espe.asignaciones.utils.ReglaNegocioException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AssignmentServiceExtendedTest {

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

    // --- helpers ---

    private VehicleAssignment assignment(UUID userId, UUID vehicleId, AssignmentStatus status, boolean active) {
        return VehicleAssignment.builder()
                .id(new AssignmentId(userId, vehicleId))
                .active(active)
                .status(status)
                .assignmentType(AssignmentType.PROPIETARIO)
                .authorizationRoleId(UUID.randomUUID())
                .authorizationRoleName("CLIENTE")
                .entryAuthorized(active)
                .build();
    }

    private UserRoleAssignmentResponse roleResponse() {
        UserRoleAssignmentResponse r = new UserRoleAssignmentResponse();
        r.setIdRole(UUID.randomUUID());
        r.setRol("CLIENTE");
        return r;
    }

    private VehiculoClientResponse vehiculoResponse(UUID vehicleId) {
        VehiculoClientResponse v = new VehiculoClientResponse();
        v.setId(vehicleId);
        v.setPlaca("ABC-1234");
        v.setMarca("Toyota");
        v.setModelo("Corolla");
        v.setTipo("Auto");
        return v;
    }

    // --- crearAsignacion ---

    @Test
    @DisplayName("crearAsignacion crea nueva asignacion en happy path")
    void crearAsignacionNueva() {
        UUID userId = UUID.randomUUID();
        UUID vehicleId = UUID.randomUUID();
        CreateAssignmentRequest request = new CreateAssignmentRequest();
        request.setUserId(userId);
        request.setVehicleId(vehicleId);

        when(externalCatalogService.validarUsuarioActivo(userId, null)).thenReturn(new UsuarioClientResponse());
        when(externalCatalogService.validarVehiculoActivo(vehicleId, null)).thenReturn(new VehiculoClientResponse());
        when(externalCatalogService.validarRolAutorizadoParaAsignacion(userId, null)).thenReturn(roleResponse());
        when(assignmentRepository.findByIdVehicleIdAndActiveTrue(vehicleId)).thenReturn(Optional.empty());
        when(assignmentRepository.findById(new AssignmentId(userId, vehicleId))).thenReturn(Optional.empty());

        VehicleAssignment saved = assignment(userId, vehicleId, AssignmentStatus.ACTIVA, true);
        when(assignmentRepository.save(any())).thenReturn(saved);

        AssignmentResponse response = service.crearAsignacion(request, null);

        assertNotNull(response);
        assertEquals(userId, response.getUserId());
        assertEquals(vehicleId, response.getVehicleId());
        verify(eventPublisher).publishEvent(any(Object.class));
    }

    @Test
    @DisplayName("crearAsignacion reactiva asignacion existente inactiva")
    void crearAsignacionReactivaExistente() {
        UUID userId = UUID.randomUUID();
        UUID vehicleId = UUID.randomUUID();
        CreateAssignmentRequest request = new CreateAssignmentRequest();
        request.setUserId(userId);
        request.setVehicleId(vehicleId);

        VehicleAssignment existing = assignment(userId, vehicleId, AssignmentStatus.FINALIZADA, false);

        when(externalCatalogService.validarUsuarioActivo(userId, null)).thenReturn(new UsuarioClientResponse());
        when(externalCatalogService.validarVehiculoActivo(vehicleId, null)).thenReturn(new VehiculoClientResponse());
        when(externalCatalogService.validarRolAutorizadoParaAsignacion(userId, null)).thenReturn(roleResponse());
        when(assignmentRepository.findByIdVehicleIdAndActiveTrue(vehicleId)).thenReturn(Optional.empty());
        when(assignmentRepository.findById(new AssignmentId(userId, vehicleId))).thenReturn(Optional.of(existing));
        when(assignmentRepository.save(any())).thenReturn(existing);

        AssignmentResponse response = service.crearAsignacion(request, null);

        assertNotNull(response);
        assertTrue(existing.isActive());
        assertEquals(AssignmentStatus.ACTIVA, existing.getStatus());
    }

    @Test
    @DisplayName("crearAsignacion rechaza si ya esta activa")
    void crearAsignacionYaActiva() {
        UUID userId = UUID.randomUUID();
        UUID vehicleId = UUID.randomUUID();
        CreateAssignmentRequest request = new CreateAssignmentRequest();
        request.setUserId(userId);
        request.setVehicleId(vehicleId);

        VehicleAssignment existing = assignment(userId, vehicleId, AssignmentStatus.ACTIVA, true);

        when(externalCatalogService.validarUsuarioActivo(userId, null)).thenReturn(new UsuarioClientResponse());
        when(externalCatalogService.validarVehiculoActivo(vehicleId, null)).thenReturn(new VehiculoClientResponse());
        when(externalCatalogService.validarRolAutorizadoParaAsignacion(userId, null)).thenReturn(roleResponse());
        when(assignmentRepository.findByIdVehicleIdAndActiveTrue(vehicleId)).thenReturn(Optional.empty());
        when(assignmentRepository.findById(new AssignmentId(userId, vehicleId))).thenReturn(Optional.of(existing));

        assertThrows(ReglaNegocioException.class, () -> service.crearAsignacion(request, null));
    }

    // --- desactivarAsignacion ---

    @Test
    @DisplayName("desactivarAsignacion finaliza una asignacion activa")
    void desactivarAsignacionHappyPath() {
        UUID userId = UUID.randomUUID();
        UUID vehicleId = UUID.randomUUID();
        VehicleAssignment a = assignment(userId, vehicleId, AssignmentStatus.ACTIVA, true);

        when(assignmentRepository.findById(new AssignmentId(userId, vehicleId))).thenReturn(Optional.of(a));
        when(assignmentRepository.save(any())).thenReturn(a);

        AssignmentResponse response = service.desactivarAsignacion(userId, vehicleId);

        assertFalse(a.isActive());
        assertEquals(AssignmentStatus.FINALIZADA, a.getStatus());
        assertFalse(a.isEntryAuthorized());
        assertNotNull(response);
        verify(eventPublisher).publishEvent(any(Object.class));
    }

    @Test
    @DisplayName("desactivarAsignacion lanza RNE si no existe")
    void desactivarAsignacionNoExiste() {
        UUID userId = UUID.randomUUID();
        UUID vehicleId = UUID.randomUUID();
        when(assignmentRepository.findById(new AssignmentId(userId, vehicleId))).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class, () -> service.desactivarAsignacion(userId, vehicleId));
    }

    @Test
    @DisplayName("desactivarAsignacion lanza RNE si ya esta inactiva")
    void desactivarAsignacionYaInactiva() {
        UUID userId = UUID.randomUUID();
        UUID vehicleId = UUID.randomUUID();
        VehicleAssignment a = assignment(userId, vehicleId, AssignmentStatus.FINALIZADA, false);

        when(assignmentRepository.findById(new AssignmentId(userId, vehicleId))).thenReturn(Optional.of(a));

        assertThrows(ReglaNegocioException.class, () -> service.desactivarAsignacion(userId, vehicleId));
    }

    // --- consultarFlota ---

    @Test
    @DisplayName("consultarFlota devuelve lista de vehiculos del usuario")
    void consultarFlotaHappyPath() {
        UUID userId = UUID.randomUUID();
        UUID vehicleId = UUID.randomUUID();
        VehicleAssignment a = assignment(userId, vehicleId, AssignmentStatus.ACTIVA, true);

        when(externalCatalogService.validarUsuarioActivo(userId, null)).thenReturn(new UsuarioClientResponse());
        when(assignmentRepository.findByIdUserIdAndActiveTrue(userId)).thenReturn(List.of(a));
        when(externalCatalogService.validarVehiculoActivo(vehicleId, null)).thenReturn(vehiculoResponse(vehicleId));

        List<FleetVehicleResponse> fleet = service.consultarFlota(userId, null);

        assertEquals(1, fleet.size());
        assertEquals(vehicleId, fleet.get(0).getVehicleId());
        assertEquals("ABC-1234", fleet.get(0).getPlaca());
    }

    @Test
    @DisplayName("consultarFlota devuelve lista vacia cuando no hay asignaciones")
    void consultarFlotaVacia() {
        UUID userId = UUID.randomUUID();
        when(externalCatalogService.validarUsuarioActivo(userId, null)).thenReturn(new UsuarioClientResponse());
        when(assignmentRepository.findByIdUserIdAndActiveTrue(userId)).thenReturn(List.of());

        List<FleetVehicleResponse> fleet = service.consultarFlota(userId, null);

        assertTrue(fleet.isEmpty());
    }

    // --- consultarAsignacionActivaPorVehiculo ---

    @Test
    @DisplayName("consultarAsignacionActivaPorVehiculo devuelve asignacion activa")
    void consultarAsignacionActivaPorVehiculoOk() {
        UUID vehicleId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        VehicleAssignment a = assignment(userId, vehicleId, AssignmentStatus.ACTIVA, true);

        when(assignmentRepository.findByIdVehicleIdAndActiveTrue(vehicleId)).thenReturn(Optional.of(a));

        AssignmentResponse response = service.consultarAsignacionActivaPorVehiculo(vehicleId);

        assertEquals(vehicleId, response.getVehicleId());
    }

    @Test
    @DisplayName("consultarAsignacionActivaPorVehiculo lanza RNE si no hay asignacion activa")
    void consultarAsignacionActivaPorVehiculoNoExiste() {
        UUID vehicleId = UUID.randomUUID();
        when(assignmentRepository.findByIdVehicleIdAndActiveTrue(vehicleId)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class,
                () -> service.consultarAsignacionActivaPorVehiculo(vehicleId));
    }

    // --- listarAsignaciones ---

    @Test
    @DisplayName("listarAsignaciones devuelve todas sin filtro")
    void listarAsignacionesTodas() {
        UUID u1 = UUID.randomUUID(), v1 = UUID.randomUUID();
        UUID u2 = UUID.randomUUID(), v2 = UUID.randomUUID();
        VehicleAssignment a1 = assignment(u1, v1, AssignmentStatus.ACTIVA, true);
        VehicleAssignment a2 = assignment(u2, v2, AssignmentStatus.FINALIZADA, false);

        when(assignmentRepository.findAll()).thenReturn(List.of(a1, a2));

        List<AssignmentResponse> list = service.listarAsignaciones(false);

        assertEquals(2, list.size());
    }

    @Test
    @DisplayName("listarAsignaciones filtra solo activas cuando soloActivas=true")
    void listarAsignacionesSoloActivas() {
        UUID u1 = UUID.randomUUID(), v1 = UUID.randomUUID();
        UUID u2 = UUID.randomUUID(), v2 = UUID.randomUUID();
        VehicleAssignment a1 = assignment(u1, v1, AssignmentStatus.ACTIVA, true);
        VehicleAssignment a2 = assignment(u2, v2, AssignmentStatus.FINALIZADA, false);

        when(assignmentRepository.findAll()).thenReturn(List.of(a1, a2));

        List<AssignmentResponse> list = service.listarAsignaciones(true);

        assertEquals(1, list.size());
        assertEquals(u1, list.get(0).getUserId());
    }

    // --- consultarTrazabilidad ---

    @Test
    @DisplayName("consultarTrazabilidad devuelve eventos de auditoria")
    void consultarTrazabilidadOk() {
        UUID userId = UUID.randomUUID();
        UUID vehicleId = UUID.randomUUID();
        AssignmentAuditEvent event = AssignmentAuditEvent.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .vehicleId(vehicleId)
                .action(ec.edu.espe.asignaciones.entities.AuditAction.CREACION)
                .timestamp(OffsetDateTime.now())
                .build();

        when(auditRepository.findByUserIdAndVehicleIdOrderByTimestampDesc(userId, vehicleId))
                .thenReturn(List.of(event));

        List<AuditEventResponse> trazabilidad = service.consultarTrazabilidad(userId, vehicleId);

        assertEquals(1, trazabilidad.size());
        assertEquals(userId, trazabilidad.get(0).getUserId());
        assertEquals(vehicleId, trazabilidad.get(0).getVehicleId());
    }

    @Test
    @DisplayName("consultarTrazabilidad devuelve lista vacia si no hay eventos")
    void consultarTrazabilidadVacia() {
        UUID userId = UUID.randomUUID();
        UUID vehicleId = UUID.randomUUID();
        when(auditRepository.findByUserIdAndVehicleIdOrderByTimestampDesc(userId, vehicleId))
                .thenReturn(List.of());

        assertTrue(service.consultarTrazabilidad(userId, vehicleId).isEmpty());
    }

    // --- modificarAsignacion ---

    @Test
    @DisplayName("modificarAsignacion actualiza alias y observacion")
    void modificarAsignacionCamposOpcionales() {
        UUID userId = UUID.randomUUID();
        UUID vehicleId = UUID.randomUUID();
        VehicleAssignment a = assignment(userId, vehicleId, AssignmentStatus.ACTIVA, true);

        UpdateAssignmentRequest request = new UpdateAssignmentRequest();
        request.setVehicleAlias("Mi carro");
        request.setObservation("obs nueva");

        when(assignmentRepository.findById(new AssignmentId(userId, vehicleId))).thenReturn(Optional.of(a));
        when(assignmentRepository.save(any())).thenReturn(a);

        AssignmentResponse response = service.modificarAsignacion(userId, vehicleId, request, null);

        assertNotNull(response);
        assertEquals("Mi carro", a.getVehicleAlias());
        assertEquals("obs nueva", a.getObservation());
        verify(eventPublisher).publishEvent(any(Object.class));
    }

    @Test
    @DisplayName("modificarAsignacion rechaza entryAuthorized=true en asignacion no ACTIVA")
    void modificarAsignacionEntryAuthorizedNoActiva() {
        UUID userId = UUID.randomUUID();
        UUID vehicleId = UUID.randomUUID();
        VehicleAssignment a = assignment(userId, vehicleId, AssignmentStatus.SUSPENDIDA, true);

        UpdateAssignmentRequest request = new UpdateAssignmentRequest();
        request.setEntryAuthorized(true);

        when(assignmentRepository.findById(new AssignmentId(userId, vehicleId))).thenReturn(Optional.of(a));

        assertThrows(ReglaNegocioException.class,
                () -> service.modificarAsignacion(userId, vehicleId, request, null));
    }

    @Test
    @DisplayName("modificarAsignacion rechaza validUntil anterior a validFrom")
    void modificarAsignacionFechaInvalida() {
        UUID userId = UUID.randomUUID();
        UUID vehicleId = UUID.randomUUID();
        VehicleAssignment a = assignment(userId, vehicleId, AssignmentStatus.ACTIVA, true);
        a.setValidFrom(OffsetDateTime.now());

        UpdateAssignmentRequest request = new UpdateAssignmentRequest();
        request.setValidUntil(OffsetDateTime.now().minusDays(1));

        when(assignmentRepository.findById(new AssignmentId(userId, vehicleId))).thenReturn(Optional.of(a));

        assertThrows(ReglaNegocioException.class,
                () -> service.modificarAsignacion(userId, vehicleId, request, null));
    }

    // --- reactivarAsignacion ---

    @Test
    @DisplayName("reactivarAsignacion happy path reactiva correctamente")
    void reactivarAsignacionHappyPath() {
        UUID userId = UUID.randomUUID();
        UUID vehicleId = UUID.randomUUID();
        VehicleAssignment a = assignment(userId, vehicleId, AssignmentStatus.FINALIZADA, false);

        when(externalCatalogService.validarUsuarioActivo(userId, null)).thenReturn(new UsuarioClientResponse());
        when(externalCatalogService.validarVehiculoActivo(vehicleId, null)).thenReturn(new VehiculoClientResponse());
        when(assignmentRepository.findByIdVehicleIdAndActiveTrue(vehicleId)).thenReturn(Optional.empty());
        when(assignmentRepository.findById(new AssignmentId(userId, vehicleId))).thenReturn(Optional.of(a));
        when(externalCatalogService.validarRolAutorizadoParaAsignacion(userId, null)).thenReturn(roleResponse());
        when(assignmentRepository.save(any())).thenReturn(a);

        AssignmentResponse response = service.reactivarAsignacion(userId, vehicleId, null);

        assertNotNull(response);
        assertTrue(a.isActive());
        assertEquals(AssignmentStatus.ACTIVA, a.getStatus());
        verify(eventPublisher).publishEvent(any(Object.class));
    }

    @Test
    @DisplayName("reactivarAsignacion lanza RNE si no existe")
    void reactivarAsignacionNoExiste() {
        UUID userId = UUID.randomUUID();
        UUID vehicleId = UUID.randomUUID();

        when(externalCatalogService.validarUsuarioActivo(userId, null)).thenReturn(new UsuarioClientResponse());
        when(externalCatalogService.validarVehiculoActivo(vehicleId, null)).thenReturn(new VehiculoClientResponse());
        when(assignmentRepository.findByIdVehicleIdAndActiveTrue(vehicleId)).thenReturn(Optional.empty());
        when(assignmentRepository.findById(new AssignmentId(userId, vehicleId))).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class,
                () -> service.reactivarAsignacion(userId, vehicleId, null));
    }

    @Test
    @DisplayName("reactivarAsignacion lanza RNE si ya esta activa")
    void reactivarAsignacionYaActiva() {
        UUID userId = UUID.randomUUID();
        UUID vehicleId = UUID.randomUUID();
        VehicleAssignment a = assignment(userId, vehicleId, AssignmentStatus.ACTIVA, true);

        when(externalCatalogService.validarUsuarioActivo(userId, null)).thenReturn(new UsuarioClientResponse());
        when(externalCatalogService.validarVehiculoActivo(vehicleId, null)).thenReturn(new VehiculoClientResponse());
        when(assignmentRepository.findByIdVehicleIdAndActiveTrue(vehicleId)).thenReturn(Optional.empty());
        when(assignmentRepository.findById(new AssignmentId(userId, vehicleId))).thenReturn(Optional.of(a));

        assertThrows(ReglaNegocioException.class,
                () -> service.reactivarAsignacion(userId, vehicleId, null));
    }
}
