package ec.edu.espe.tickets.services;

import ec.edu.espe.tickets.audit.AuditRequest;
import ec.edu.espe.tickets.dtos.*;
import ec.edu.espe.tickets.entities.EstadoTicket;
import ec.edu.espe.tickets.entities.Ticket;
import ec.edu.espe.tickets.repositories.TicketRepository;
import ec.edu.espe.tickets.services.impl.TicketServiceImpl;
import ec.edu.espe.tickets.utils.RecursoNoEncontradoException;
import ec.edu.espe.tickets.utils.ReglaNegocioException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class TicketServiceImplTest {

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
        service = new TicketServiceImpl(
                ticketRepository, catalogo, calculadoraTarifa, generadorCodigo, eventPublisher);
    }

    // --- helpers ---

    private VehiculoClientResponse vehiculo(UUID id, String tipo, boolean activo) {
        VehiculoClientResponse v = new VehiculoClientResponse();
        v.setId(id);
        v.setPlaca("ABC-1234");
        v.setTipo(tipo);
        v.setActivo(activo);
        return v;
    }

    private AsignacionActivaResponse asignacion(UUID userId, boolean active, boolean entryAuth) {
        AsignacionActivaResponse a = new AsignacionActivaResponse();
        a.setUserId(userId);
        a.setActive(active);
        a.setStatus("ACTIVA");
        a.setEntryAuthorized(entryAuth);
        a.setRolAutorizacion("CLIENTE");
        a.setAssignmentType("PROPIETARIO");
        return a;
    }

    private EspacioClientResponse espacio(UUID id, String tipo, boolean activo, String estado) {
        EspacioClientResponse e = new EspacioClientResponse();
        e.setId(id);
        e.setCodigo("E-01");
        e.setTipo(tipo);
        e.setActivo(activo);
        e.setEstado(estado);
        return e;
    }

    private Ticket ticket(UUID id, EstadoTicket estado) {
        return Ticket.builder()
                .id(id)
                .codigo("T-001")
                .idVehiculo(UUID.randomUUID())
                .idEspacio(UUID.randomUUID())
                .tipoVehiculo("Auto")
                .tipoEspacio("AUTO")
                .categoriaTarifa("CLIENTE")
                .fechaHoraIngreso(OffsetDateTime.now().minusHours(2))
                .estadoTicket(estado)
                .valorRecaudado(BigDecimal.ZERO)
                .build();
    }

    // --- registrarIngreso ---

    @Test
    @DisplayName("registrarIngreso happy path crea ticket correctamente")
    void registrarIngresoOk() {
        UUID vehiculoId = UUID.randomUUID();
        UUID espacioId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        RegistrarIngresoRequest request = new RegistrarIngresoRequest();
        request.setPlaca("abc-1234");
        request.setIdEspacio(espacioId);

        when(catalogo.obtenerVehiculoPorPlaca("ABC-1234")).thenReturn(vehiculo(vehiculoId, "Auto", true));
        when(catalogo.obtenerAsignacionActivaPorVehiculo(vehiculoId)).thenReturn(asignacion(userId, true, true));
        when(ticketRepository.findByIdVehiculoAndEstadoTicket(vehiculoId, EstadoTicket.ACTIVO)).thenReturn(Optional.empty());
        when(catalogo.obtenerEspacio(espacioId)).thenReturn(espacio(espacioId, "AUTO", true, "DISPONIBLE"));
        when(ticketRepository.findByIdEspacioAndEstadoTicket(espacioId, EstadoTicket.ACTIVO)).thenReturn(Optional.empty());
        when(generadorCodigo.generar()).thenReturn("T-001");

        Ticket saved = ticket(UUID.randomUUID(), EstadoTicket.ACTIVO);
        saved.setIdVehiculo(vehiculoId);
        saved.setIdEspacio(espacioId);
        when(ticketRepository.saveAndFlush(any())).thenReturn(saved);

        TicketResponse response = service.registrarIngreso(request, UUID.randomUUID());

        assertNotNull(response);
        verify(catalogo).cambiarEstadoEspacio(espacioId, "OCUPADO");
        verify(eventPublisher).publishEvent(any(AuditRequest.class));
    }

    @Test
    @DisplayName("registrarIngreso lanza RNE si vehiculo inactivo")
    void registrarIngresoVehiculoInactivo() {
        UUID vehiculoId = UUID.randomUUID();
        RegistrarIngresoRequest request = new RegistrarIngresoRequest();
        request.setPlaca("ABC-1234");
        request.setIdEspacio(UUID.randomUUID());

        when(catalogo.obtenerVehiculoPorPlaca("ABC-1234")).thenReturn(vehiculo(vehiculoId, "Auto", false));

        assertThrows(ReglaNegocioException.class, () -> service.registrarIngreso(request, null));
    }

    @Test
    @DisplayName("registrarIngreso lanza RNE si asignacion inactiva")
    void registrarIngresoAsignacionInactiva() {
        UUID vehiculoId = UUID.randomUUID();
        RegistrarIngresoRequest request = new RegistrarIngresoRequest();
        request.setPlaca("ABC-1234");
        request.setIdEspacio(UUID.randomUUID());

        when(catalogo.obtenerVehiculoPorPlaca("ABC-1234")).thenReturn(vehiculo(vehiculoId, "Auto", true));
        AsignacionActivaResponse a = asignacion(UUID.randomUUID(), false, true);
        when(catalogo.obtenerAsignacionActivaPorVehiculo(vehiculoId)).thenReturn(a);

        assertThrows(ReglaNegocioException.class, () -> service.registrarIngreso(request, null));
    }

    @Test
    @DisplayName("registrarIngreso lanza RNE si no tiene autorizacion de ingreso")
    void registrarIngresoSinAutorizacion() {
        UUID vehiculoId = UUID.randomUUID();
        RegistrarIngresoRequest request = new RegistrarIngresoRequest();
        request.setPlaca("ABC-1234");
        request.setIdEspacio(UUID.randomUUID());

        when(catalogo.obtenerVehiculoPorPlaca("ABC-1234")).thenReturn(vehiculo(vehiculoId, "Auto", true));
        when(catalogo.obtenerAsignacionActivaPorVehiculo(vehiculoId)).thenReturn(asignacion(UUID.randomUUID(), true, false));

        assertThrows(ReglaNegocioException.class, () -> service.registrarIngreso(request, null));
    }

    @Test
    @DisplayName("registrarIngreso lanza RNE si vehiculo ya tiene ticket activo")
    void registrarIngresoVehiculoYaActivo() {
        UUID vehiculoId = UUID.randomUUID();
        UUID espacioId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        RegistrarIngresoRequest request = new RegistrarIngresoRequest();
        request.setPlaca("ABC-1234");
        request.setIdEspacio(espacioId);

        when(catalogo.obtenerVehiculoPorPlaca("ABC-1234")).thenReturn(vehiculo(vehiculoId, "Auto", true));
        when(catalogo.obtenerAsignacionActivaPorVehiculo(vehiculoId)).thenReturn(asignacion(userId, true, true));
        when(ticketRepository.findByIdVehiculoAndEstadoTicket(vehiculoId, EstadoTicket.ACTIVO))
                .thenReturn(Optional.of(ticket(UUID.randomUUID(), EstadoTicket.ACTIVO)));

        assertThrows(ReglaNegocioException.class, () -> service.registrarIngreso(request, null));
    }

    @Test
    @DisplayName("registrarIngreso lanza RNE si espacio ya tiene ticket activo")
    void registrarIngresoEspacioYaActivo() {
        UUID vehiculoId = UUID.randomUUID();
        UUID espacioId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        RegistrarIngresoRequest request = new RegistrarIngresoRequest();
        request.setPlaca("ABC-1234");
        request.setIdEspacio(espacioId);

        when(catalogo.obtenerVehiculoPorPlaca("ABC-1234")).thenReturn(vehiculo(vehiculoId, "Auto", true));
        when(catalogo.obtenerAsignacionActivaPorVehiculo(vehiculoId)).thenReturn(asignacion(userId, true, true));
        when(ticketRepository.findByIdVehiculoAndEstadoTicket(vehiculoId, EstadoTicket.ACTIVO)).thenReturn(Optional.empty());
        when(catalogo.obtenerEspacio(espacioId)).thenReturn(espacio(espacioId, "AUTO", true, "DISPONIBLE"));
        when(ticketRepository.findByIdEspacioAndEstadoTicket(espacioId, EstadoTicket.ACTIVO))
                .thenReturn(Optional.of(ticket(UUID.randomUUID(), EstadoTicket.ACTIVO)));

        assertThrows(ReglaNegocioException.class, () -> service.registrarIngreso(request, null));
    }

    @Test
    @DisplayName("registrarIngreso lanza RNE si espacio inactivo")
    void registrarIngresoEspacioInactivo() {
        UUID vehiculoId = UUID.randomUUID();
        UUID espacioId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        RegistrarIngresoRequest request = new RegistrarIngresoRequest();
        request.setPlaca("ABC-1234");
        request.setIdEspacio(espacioId);

        when(catalogo.obtenerVehiculoPorPlaca("ABC-1234")).thenReturn(vehiculo(vehiculoId, "Auto", true));
        when(catalogo.obtenerAsignacionActivaPorVehiculo(vehiculoId)).thenReturn(asignacion(userId, true, true));
        when(ticketRepository.findByIdVehiculoAndEstadoTicket(vehiculoId, EstadoTicket.ACTIVO)).thenReturn(Optional.empty());
        when(catalogo.obtenerEspacio(espacioId)).thenReturn(espacio(espacioId, "AUTO", false, "DISPONIBLE"));

        assertThrows(ReglaNegocioException.class, () -> service.registrarIngreso(request, null));
    }

    @Test
    @DisplayName("registrarIngreso lanza RNE si tipos incompatibles")
    void registrarIngresoTiposIncompatibles() {
        UUID vehiculoId = UUID.randomUUID();
        UUID espacioId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        RegistrarIngresoRequest request = new RegistrarIngresoRequest();
        request.setPlaca("ABC-1234");
        request.setIdEspacio(espacioId);

        when(catalogo.obtenerVehiculoPorPlaca("ABC-1234")).thenReturn(vehiculo(vehiculoId, "Motocicleta", true));
        when(catalogo.obtenerAsignacionActivaPorVehiculo(vehiculoId)).thenReturn(asignacion(userId, true, true));
        when(ticketRepository.findByIdVehiculoAndEstadoTicket(vehiculoId, EstadoTicket.ACTIVO)).thenReturn(Optional.empty());
        when(catalogo.obtenerEspacio(espacioId)).thenReturn(espacio(espacioId, "AUTO", true, "DISPONIBLE"));
        when(ticketRepository.findByIdEspacioAndEstadoTicket(espacioId, EstadoTicket.ACTIVO)).thenReturn(Optional.empty());

        assertThrows(ReglaNegocioException.class, () -> service.registrarIngreso(request, null));
    }

    // --- pagar ---

    @Test
    @DisplayName("pagar happy path calcula valor y cambia estado a PAGADO")
    void pagarOk() {
        UUID ticketId = UUID.randomUUID();
        Ticket t = ticket(ticketId, EstadoTicket.ACTIVO);

        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(t));
        when(calculadoraTarifa.calcular(any(), any(), any(), any(), any())).thenReturn(new BigDecimal("3.00"));
        when(ticketRepository.save(any())).thenReturn(t);

        TicketResponse response = service.pagar(ticketId, UUID.randomUUID());

        assertNotNull(response);
        assertEquals(EstadoTicket.PAGADO, t.getEstadoTicket());
        assertEquals(new BigDecimal("3.00"), t.getValorRecaudado());
        verify(catalogo).cambiarEstadoEspacio(t.getIdEspacio(), "DISPONIBLE");
        verify(eventPublisher).publishEvent(any(AuditRequest.class));
    }

    @Test
    @DisplayName("pagar lanza RNE si ticket no es ACTIVO")
    void pagarTicketNoActivo() {
        UUID ticketId = UUID.randomUUID();
        Ticket t = ticket(ticketId, EstadoTicket.PAGADO);

        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(t));

        assertThrows(ReglaNegocioException.class, () -> service.pagar(ticketId, null));
    }

    @Test
    @DisplayName("pagar lanza RNE si ticket no existe")
    void pagarTicketNoExiste() {
        UUID ticketId = UUID.randomUUID();
        when(ticketRepository.findById(ticketId)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class, () -> service.pagar(ticketId, null));
    }

    // --- anular ---

    @Test
    @DisplayName("anular happy path cambia estado a ANULADO")
    void anularOk() {
        UUID ticketId = UUID.randomUUID();
        Ticket t = ticket(ticketId, EstadoTicket.ACTIVO);

        AnularTicketRequest request = new AnularTicketRequest();
        request.setMotivo("Error de registro");

        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(t));
        when(ticketRepository.save(any())).thenReturn(t);

        TicketResponse response = service.anular(ticketId, request, UUID.randomUUID());

        assertNotNull(response);
        assertEquals(EstadoTicket.ANULADO, t.getEstadoTicket());
        assertEquals("Error de registro", t.getMotivoAnulacion());
        verify(catalogo).cambiarEstadoEspacio(t.getIdEspacio(), "DISPONIBLE");
    }

    @Test
    @DisplayName("anular lanza RNE si ticket no es ACTIVO")
    void anularTicketNoActivo() {
        UUID ticketId = UUID.randomUUID();
        Ticket t = ticket(ticketId, EstadoTicket.ANULADO);

        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(t));
        AnularTicketRequest request = new AnularTicketRequest();
        request.setMotivo("motivo");

        assertThrows(ReglaNegocioException.class, () -> service.anular(ticketId, request, null));
    }

    // --- obtenerPorId ---

    @Test
    @DisplayName("obtenerPorId devuelve ticket existente")
    void obtenerPorIdOk() {
        UUID ticketId = UUID.randomUUID();
        Ticket t = ticket(ticketId, EstadoTicket.ACTIVO);

        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(t));

        TicketResponse response = service.obtenerPorId(ticketId);
        assertNotNull(response);
    }

    @Test
    @DisplayName("obtenerPorId lanza RNE si no existe")
    void obtenerPorIdNoExiste() {
        UUID ticketId = UUID.randomUUID();
        when(ticketRepository.findById(ticketId)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class, () -> service.obtenerPorId(ticketId));
    }

    // --- obtenerPorCodigo ---

    @Test
    @DisplayName("obtenerPorCodigo devuelve ticket")
    void obtenerPorCodigoOk() {
        Ticket t = ticket(UUID.randomUUID(), EstadoTicket.PAGADO);
        when(ticketRepository.findByCodigo("T-001")).thenReturn(Optional.of(t));

        TicketResponse response = service.obtenerPorCodigo("T-001");
        assertNotNull(response);
    }

    @Test
    @DisplayName("obtenerPorCodigo lanza RNE si no existe")
    void obtenerPorCodigoNoExiste() {
        when(ticketRepository.findByCodigo("NOPE")).thenReturn(Optional.empty());
        assertThrows(RecursoNoEncontradoException.class, () -> service.obtenerPorCodigo("NOPE"));
    }

    // --- listar ---

    @Test
    @DisplayName("listar sin filtro retorna todos")
    void listarSinFiltro() {
        Ticket t = ticket(UUID.randomUUID(), EstadoTicket.ACTIVO);
        Page<Ticket> page = new PageImpl<>(List.of(t));
        when(ticketRepository.findAllByOrderByFechaHoraIngresoDesc(any(Pageable.class))).thenReturn(page);

        Page<TicketResponse> result = service.listar(null, Pageable.unpaged());
        assertEquals(1, result.getTotalElements());
    }

    @Test
    @DisplayName("listar con filtro por estado")
    void listarConFiltro() {
        Ticket t = ticket(UUID.randomUUID(), EstadoTicket.PAGADO);
        Page<Ticket> page = new PageImpl<>(List.of(t));
        when(ticketRepository.findByEstadoTicketOrderByFechaHoraIngresoDesc(eq(EstadoTicket.PAGADO), any(Pageable.class))).thenReturn(page);

        Page<TicketResponse> result = service.listar(EstadoTicket.PAGADO, Pageable.unpaged());
        assertEquals(1, result.getTotalElements());
    }

    // --- obtenerActivoPorEspacio ---

    @Test
    @DisplayName("obtenerActivoPorEspacio retorna ticket activo")
    void obtenerActivoPorEspacioOk() {
        UUID espacioId = UUID.randomUUID();
        Ticket t = ticket(UUID.randomUUID(), EstadoTicket.ACTIVO);
        t.setIdEspacio(espacioId);

        when(ticketRepository.findByIdEspacioAndEstadoTicket(espacioId, EstadoTicket.ACTIVO)).thenReturn(Optional.of(t));

        TicketResponse response = service.obtenerActivoPorEspacio(espacioId);
        assertNotNull(response);
    }

    @Test
    @DisplayName("obtenerActivoPorEspacio lanza RNE si no hay ticket activo")
    void obtenerActivoPorEspacioNoExiste() {
        UUID espacioId = UUID.randomUUID();
        when(ticketRepository.findByIdEspacioAndEstadoTicket(espacioId, EstadoTicket.ACTIVO)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class, () -> service.obtenerActivoPorEspacio(espacioId));
    }
}
