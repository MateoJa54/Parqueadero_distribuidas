package ec.edu.espe.tickets.services;

import ec.edu.espe.tickets.audit.AuditRequest;
import ec.edu.espe.tickets.dtos.*;
import ec.edu.espe.tickets.entities.EstadoTicket;
import ec.edu.espe.tickets.entities.Ticket;
import ec.edu.espe.tickets.repositories.TicketRepository;
import ec.edu.espe.tickets.services.impl.TicketServiceImpl;
import ec.edu.espe.tickets.utils.ReglaNegocioException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Cubre ramas que el test principal no ejercita:
 * - registrarIngreso con DataIntegrityViolationException (concurrencia)
 * - registrarIngreso con asignacion sin rolAutorizacion (usa assignmentType)
 * - validarAsignacion: status != ACTIVA
 * - validarAsignacion: validFrom en el futuro
 * - validarAsignacion: validUntil expirado
 * - validarCompatibilidad: tipo vehiculo desconocido (esperado == null)
 */
class TicketServiceImplBranchesTest {

    private TicketRepository ticketRepository;
    private CatalogoExternoService catalogo;
    private CalculadoraTarifa calculadoraTarifa;
    private GeneradorCodigoTicket generadorCodigo;
    private ApplicationEventPublisher eventPublisher;
    private TicketServiceImpl service;

    @BeforeEach
    void setUp() {
        ticketRepository = mock(TicketRepository.class);
        catalogo = mock(CatalogoExternoService.class);
        calculadoraTarifa = mock(CalculadoraTarifa.class);
        generadorCodigo = mock(GeneradorCodigoTicket.class);
        eventPublisher = mock(ApplicationEventPublisher.class);
        service = new TicketServiceImpl(ticketRepository, catalogo, calculadoraTarifa, generadorCodigo, eventPublisher);
    }

    private VehiculoClientResponse vehiculoActivo(UUID id, String tipo) {
        VehiculoClientResponse v = new VehiculoClientResponse();
        v.setId(id);
        v.setPlaca("ABC-1234");
        v.setTipo(tipo);
        v.setActivo(true);
        return v;
    }

    private AsignacionActivaResponse asignacionOk(UUID userId) {
        AsignacionActivaResponse a = new AsignacionActivaResponse();
        a.setUserId(userId);
        a.setActive(true);
        a.setStatus("ACTIVA");
        a.setEntryAuthorized(true);
        a.setRolAutorizacion("CLIENTE");
        a.setAssignmentType("PROPIETARIO");
        return a;
    }

    private EspacioClientResponse espacioDisponible(UUID id, String tipo) {
        EspacioClientResponse e = new EspacioClientResponse();
        e.setId(id);
        e.setCodigo("E-01");
        e.setTipo(tipo);
        e.setActivo(true);
        e.setEstado("DISPONIBLE");
        return e;
    }

    // ---- DataIntegrityViolationException => ReglaNegocioException ----

    @Test
    void registrarIngreso_conflictoConcurrencia_lanzaReglaNegocio() {
        UUID vehiculoId = UUID.randomUUID();
        UUID espacioId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        RegistrarIngresoRequest req = new RegistrarIngresoRequest();
        req.setPlaca("ABC-1234");
        req.setIdEspacio(espacioId);

        when(catalogo.obtenerVehiculoPorPlaca("ABC-1234")).thenReturn(vehiculoActivo(vehiculoId, "Auto"));
        when(catalogo.obtenerAsignacionActivaPorVehiculo(vehiculoId)).thenReturn(asignacionOk(userId));
        when(ticketRepository.findByIdVehiculoAndEstadoTicket(vehiculoId, EstadoTicket.ACTIVO)).thenReturn(Optional.empty());
        when(catalogo.obtenerEspacio(espacioId)).thenReturn(espacioDisponible(espacioId, "AUTO"));
        when(ticketRepository.findByIdEspacioAndEstadoTicket(espacioId, EstadoTicket.ACTIVO)).thenReturn(Optional.empty());
        when(generadorCodigo.generar()).thenReturn("TKT-000001");
        when(ticketRepository.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("unique constraint"));

        UUID idEmpleado = UUID.randomUUID();
        assertThrows(ReglaNegocioException.class, () -> service.registrarIngreso(req, idEmpleado));
    }

    // ---- rolAutorizacion nulo => usa assignmentType ----

    @Test
    void registrarIngreso_sinRolAutorizacion_usaAssignmentType() {
        UUID vehiculoId = UUID.randomUUID();
        UUID espacioId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        RegistrarIngresoRequest req = new RegistrarIngresoRequest();
        req.setPlaca("ABC-1234");
        req.setIdEspacio(espacioId);

        AsignacionActivaResponse a = new AsignacionActivaResponse();
        a.setUserId(userId);
        a.setActive(true);
        a.setStatus("ACTIVA");
        a.setEntryAuthorized(true);
        a.setRolAutorizacion(null); // sin rol
        a.setAssignmentType("TEMPORAL");

        when(catalogo.obtenerVehiculoPorPlaca("ABC-1234")).thenReturn(vehiculoActivo(vehiculoId, "Auto"));
        when(catalogo.obtenerAsignacionActivaPorVehiculo(vehiculoId)).thenReturn(a);
        when(ticketRepository.findByIdVehiculoAndEstadoTicket(vehiculoId, EstadoTicket.ACTIVO)).thenReturn(Optional.empty());
        when(catalogo.obtenerEspacio(espacioId)).thenReturn(espacioDisponible(espacioId, "AUTO"));
        when(ticketRepository.findByIdEspacioAndEstadoTicket(espacioId, EstadoTicket.ACTIVO)).thenReturn(Optional.empty());
        when(generadorCodigo.generar()).thenReturn("TKT-000001");

        Ticket saved = Ticket.builder()
                .id(UUID.randomUUID()).codigo("TKT-000001")
                .idVehiculo(vehiculoId).idEspacio(espacioId)
                .tipoVehiculo("Auto").tipoEspacio("AUTO")
                .categoriaTarifa("TEMPORAL")
                .fechaHoraIngreso(OffsetDateTime.now())
                .estadoTicket(EstadoTicket.ACTIVO)
                .valorRecaudado(BigDecimal.ZERO)
                .build();
        when(ticketRepository.saveAndFlush(any())).thenReturn(saved);

        TicketResponse response = service.registrarIngreso(req, UUID.randomUUID());

        assertNotNull(response);
        assertEquals("TEMPORAL", response.getCategoriaTarifa());
        verify(eventPublisher).publishEvent(any(AuditRequest.class));
    }

    // ---- rolAutorizacion en blanco => usa assignmentType ----

    @Test
    void registrarIngreso_rolAutorizacionBlanco_usaAssignmentType() {
        UUID vehiculoId = UUID.randomUUID();
        UUID espacioId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        RegistrarIngresoRequest req = new RegistrarIngresoRequest();
        req.setPlaca("ABC-1234");
        req.setIdEspacio(espacioId);

        AsignacionActivaResponse a = new AsignacionActivaResponse();
        a.setUserId(userId);
        a.setActive(true);
        a.setStatus("ACTIVA");
        a.setEntryAuthorized(true);
        a.setRolAutorizacion("   "); // blanco
        a.setAssignmentType("AUTORIZADO");

        when(catalogo.obtenerVehiculoPorPlaca("ABC-1234")).thenReturn(vehiculoActivo(vehiculoId, "Auto"));
        when(catalogo.obtenerAsignacionActivaPorVehiculo(vehiculoId)).thenReturn(a);
        when(ticketRepository.findByIdVehiculoAndEstadoTicket(vehiculoId, EstadoTicket.ACTIVO)).thenReturn(Optional.empty());
        when(catalogo.obtenerEspacio(espacioId)).thenReturn(espacioDisponible(espacioId, "AUTO"));
        when(ticketRepository.findByIdEspacioAndEstadoTicket(espacioId, EstadoTicket.ACTIVO)).thenReturn(Optional.empty());
        when(generadorCodigo.generar()).thenReturn("TKT-000002");

        Ticket saved = Ticket.builder()
                .id(UUID.randomUUID()).codigo("TKT-000002")
                .idVehiculo(vehiculoId).idEspacio(espacioId)
                .tipoVehiculo("Auto").tipoEspacio("AUTO")
                .categoriaTarifa("AUTORIZADO")
                .fechaHoraIngreso(OffsetDateTime.now())
                .estadoTicket(EstadoTicket.ACTIVO)
                .valorRecaudado(BigDecimal.ZERO)
                .build();
        when(ticketRepository.saveAndFlush(any())).thenReturn(saved);

        TicketResponse response = service.registrarIngreso(req, UUID.randomUUID());
        assertEquals("AUTORIZADO", response.getCategoriaTarifa());
    }

    // ---- validarAsignacion: status != ACTIVA ----

    @Test
    void registrarIngreso_statusNoActiva_lanzaReglaNegocio() {
        UUID vehiculoId = UUID.randomUUID();
        RegistrarIngresoRequest req = new RegistrarIngresoRequest();
        req.setPlaca("ABC-1234");
        req.setIdEspacio(UUID.randomUUID());

        AsignacionActivaResponse a = new AsignacionActivaResponse();
        a.setUserId(UUID.randomUUID());
        a.setActive(true);
        a.setStatus("SUSPENDIDA"); // != ACTIVA
        a.setEntryAuthorized(true);

        when(catalogo.obtenerVehiculoPorPlaca("ABC-1234")).thenReturn(vehiculoActivo(vehiculoId, "Auto"));
        when(catalogo.obtenerAsignacionActivaPorVehiculo(vehiculoId)).thenReturn(a);

        assertThrows(ReglaNegocioException.class, () -> service.registrarIngreso(req, null));
    }

    // ---- validarAsignacion: validFrom en el futuro ----

    @Test
    void registrarIngreso_validFromEnFuturo_lanzaReglaNegocio() {
        UUID vehiculoId = UUID.randomUUID();
        RegistrarIngresoRequest req = new RegistrarIngresoRequest();
        req.setPlaca("ABC-1234");
        req.setIdEspacio(UUID.randomUUID());

        AsignacionActivaResponse a = new AsignacionActivaResponse();
        a.setUserId(UUID.randomUUID());
        a.setActive(true);
        a.setStatus("ACTIVA");
        a.setEntryAuthorized(true);
        a.setValidFrom(OffsetDateTime.now().plusDays(1)); // futuro

        when(catalogo.obtenerVehiculoPorPlaca("ABC-1234")).thenReturn(vehiculoActivo(vehiculoId, "Auto"));
        when(catalogo.obtenerAsignacionActivaPorVehiculo(vehiculoId)).thenReturn(a);

        assertThrows(ReglaNegocioException.class, () -> service.registrarIngreso(req, null));
    }

    // ---- validarAsignacion: validUntil expirado ----

    @Test
    void registrarIngreso_validUntilExpirado_lanzaReglaNegocio() {
        UUID vehiculoId = UUID.randomUUID();
        RegistrarIngresoRequest req = new RegistrarIngresoRequest();
        req.setPlaca("ABC-1234");
        req.setIdEspacio(UUID.randomUUID());

        AsignacionActivaResponse a = new AsignacionActivaResponse();
        a.setUserId(UUID.randomUUID());
        a.setActive(true);
        a.setStatus("ACTIVA");
        a.setEntryAuthorized(true);
        a.setValidFrom(OffsetDateTime.now().minusDays(5));
        a.setValidUntil(OffsetDateTime.now().minusDays(1)); // expirado

        when(catalogo.obtenerVehiculoPorPlaca("ABC-1234")).thenReturn(vehiculoActivo(vehiculoId, "Auto"));
        when(catalogo.obtenerAsignacionActivaPorVehiculo(vehiculoId)).thenReturn(a);

        assertThrows(ReglaNegocioException.class, () -> service.registrarIngreso(req, null));
    }

    // ---- validarAsignacion: validFrom nulo, validUntil nulo => no lanza ----

    @Test
    void registrarIngreso_validFromYUntilNulos_pasaValidacion() {
        UUID vehiculoId = UUID.randomUUID();
        UUID espacioId = UUID.randomUUID();

        RegistrarIngresoRequest req = new RegistrarIngresoRequest();
        req.setPlaca("ABC-1234");
        req.setIdEspacio(espacioId);

        AsignacionActivaResponse a = new AsignacionActivaResponse();
        a.setUserId(UUID.randomUUID());
        a.setActive(true);
        a.setStatus("ACTIVA");
        a.setEntryAuthorized(true);
        a.setValidFrom(null);
        a.setValidUntil(null);
        a.setRolAutorizacion("CLIENTE");

        when(catalogo.obtenerVehiculoPorPlaca("ABC-1234")).thenReturn(vehiculoActivo(vehiculoId, "Auto"));
        when(catalogo.obtenerAsignacionActivaPorVehiculo(vehiculoId)).thenReturn(a);
        when(ticketRepository.findByIdVehiculoAndEstadoTicket(vehiculoId, EstadoTicket.ACTIVO)).thenReturn(Optional.empty());
        when(catalogo.obtenerEspacio(espacioId)).thenReturn(espacioDisponible(espacioId, "AUTO"));
        when(ticketRepository.findByIdEspacioAndEstadoTicket(espacioId, EstadoTicket.ACTIVO)).thenReturn(Optional.empty());
        when(generadorCodigo.generar()).thenReturn("TKT-000003");

        Ticket saved = Ticket.builder()
                .id(UUID.randomUUID()).codigo("TKT-000003")
                .idVehiculo(vehiculoId).idEspacio(espacioId)
                .tipoVehiculo("Auto").tipoEspacio("AUTO")
                .categoriaTarifa("CLIENTE")
                .fechaHoraIngreso(OffsetDateTime.now())
                .estadoTicket(EstadoTicket.ACTIVO)
                .valorRecaudado(BigDecimal.ZERO)
                .build();
        when(ticketRepository.saveAndFlush(any())).thenReturn(saved);

        TicketResponse response = service.registrarIngreso(req, UUID.randomUUID());
        assertNotNull(response);
    }

    // ---- validarCompatibilidad: tipo desconocido (esperado == null) ----

    @Test
    void registrarIngreso_tipoVehiculoDesconocido_mensajeSinEsperado() {
        UUID vehiculoId = UUID.randomUUID();
        UUID espacioId = UUID.randomUUID();

        RegistrarIngresoRequest req = new RegistrarIngresoRequest();
        req.setPlaca("ABC-1234");
        req.setIdEspacio(espacioId);

        when(catalogo.obtenerVehiculoPorPlaca("ABC-1234")).thenReturn(vehiculoActivo(vehiculoId, "TipoRaro"));
        when(catalogo.obtenerAsignacionActivaPorVehiculo(vehiculoId)).thenReturn(asignacionOk(UUID.randomUUID()));
        when(ticketRepository.findByIdVehiculoAndEstadoTicket(vehiculoId, EstadoTicket.ACTIVO)).thenReturn(Optional.empty());
        when(catalogo.obtenerEspacio(espacioId)).thenReturn(espacioDisponible(espacioId, "AUTO"));
        when(ticketRepository.findByIdEspacioAndEstadoTicket(espacioId, EstadoTicket.ACTIVO)).thenReturn(Optional.empty());

        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class,
                () -> service.registrarIngreso(req, null));
        assertTrue(ex.getMessage().contains("no reconocido"));
    }

    // ---- espacio no disponible (estado != DISPONIBLE) ----

    @Test
    void registrarIngreso_espacioOcupado_lanzaReglaNegocio() {
        UUID vehiculoId = UUID.randomUUID();
        UUID espacioId = UUID.randomUUID();

        RegistrarIngresoRequest req = new RegistrarIngresoRequest();
        req.setPlaca("ABC-1234");
        req.setIdEspacio(espacioId);

        EspacioClientResponse e = new EspacioClientResponse();
        e.setId(espacioId);
        e.setCodigo("E-01");
        e.setTipo("AUTO");
        e.setActivo(true);
        e.setEstado("OCUPADO"); // no disponible

        when(catalogo.obtenerVehiculoPorPlaca("ABC-1234")).thenReturn(vehiculoActivo(vehiculoId, "Auto"));
        when(catalogo.obtenerAsignacionActivaPorVehiculo(vehiculoId)).thenReturn(asignacionOk(UUID.randomUUID()));
        when(ticketRepository.findByIdVehiculoAndEstadoTicket(vehiculoId, EstadoTicket.ACTIVO)).thenReturn(Optional.empty());
        when(catalogo.obtenerEspacio(espacioId)).thenReturn(e);

        assertThrows(ReglaNegocioException.class, () -> service.registrarIngreso(req, null));
    }
}
