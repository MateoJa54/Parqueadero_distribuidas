package ec.edu.espe.tickets.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import ec.edu.espe.tickets.dtos.AnularTicketRequest;
import ec.edu.espe.tickets.dtos.RegistrarIngresoRequest;
import ec.edu.espe.tickets.dtos.TicketResponse;
import ec.edu.espe.tickets.entities.EstadoTicket;
import ec.edu.espe.tickets.services.TicketService;
import ec.edu.espe.tickets.utils.GlobalExceptionHandler;
import ec.edu.espe.tickets.utils.RecursoNoEncontradoException;
import ec.edu.espe.tickets.utils.ReglaNegocioException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class TicketControllerTest {

    private static final String EMPLEADO_ID = "3fa85f64-5717-4562-b3fc-2c963f66afa6";

    private TicketService ticketService;
    private MockMvc mockMvc;
    private ObjectMapper mapper;

    @BeforeEach
    void setUp() {
        ticketService = mock(TicketService.class);
        TicketController controller = new TicketController(ticketService);

        mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(
                        new AuthenticationPrincipalArgumentResolver(),
                        new PageableHandlerMethodArgumentResolver())
                .build();
    }

    private UsernamePasswordAuthenticationToken empleadoAuth() {
        return new UsernamePasswordAuthenticationToken(EMPLEADO_ID, null, List.of());
    }

    private void withEmpleadoAuth(MockHttpServletRequestBuilder builder) throws Exception {
        SecurityContextHolder.getContext().setAuthentication(empleadoAuth());
    }

    private TicketResponse sampleResponse(UUID id, EstadoTicket estado) {
        return TicketResponse.builder()
                .id(id)
                .codigo("TKT-000001")
                .idEspacio(UUID.randomUUID())
                .estadoTicket(estado)
                .idEmpleado(UUID.fromString(EMPLEADO_ID))
                .valorRecaudado(BigDecimal.ZERO)
                .fechaHoraIngreso(OffsetDateTime.now())
                .build();
    }

    // ---- POST /api/v1/tickets ----

    @Test
    void registrarIngreso_retorna201() throws Exception {
        UUID ticketId = UUID.randomUUID();
        RegistrarIngresoRequest request = new RegistrarIngresoRequest();
        request.setPlaca("ABC-1234");
        request.setIdEspacio(UUID.randomUUID());

        when(ticketService.registrarIngreso(any(), any())).thenReturn(sampleResponse(ticketId, EstadoTicket.ACTIVO));

        mockMvc.perform(post("/api/v1/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(request))
                        .with(req -> { SecurityContextHolder.getContext().setAuthentication(empleadoAuth()); return req; }))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estadoTicket").value("ACTIVO"));
    }

    @Test
    void registrarIngreso_servicioLanzaReglaNegocio_retorna409() throws Exception {
        RegistrarIngresoRequest request = new RegistrarIngresoRequest();
        request.setPlaca("ABC-1234");
        request.setIdEspacio(UUID.randomUUID());

        when(ticketService.registrarIngreso(any(), any()))
                .thenThrow(new ReglaNegocioException("vehiculo inactivo"));

        mockMvc.perform(post("/api/v1/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(request))
                        .with(req -> { SecurityContextHolder.getContext().setAuthentication(empleadoAuth()); return req; }))
                .andExpect(status().isConflict());
    }

    // ---- PATCH /api/v1/tickets/{id}/pagar ----

    @Test
    void pagar_retorna200() throws Exception {
        UUID ticketId = UUID.randomUUID();
        when(ticketService.pagar(eq(ticketId), any()))
                .thenReturn(sampleResponse(ticketId, EstadoTicket.PAGADO));

        mockMvc.perform(patch("/api/v1/tickets/{id}/pagar", ticketId)
                        .with(req -> { SecurityContextHolder.getContext().setAuthentication(empleadoAuth()); return req; }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estadoTicket").value("PAGADO"));
    }

    @Test
    void pagar_ticketNoExiste_lanzaException() throws Exception {
        UUID ticketId = UUID.randomUUID();
        when(ticketService.pagar(eq(ticketId), any()))
                .thenThrow(new RecursoNoEncontradoException("no existe"));

        mockMvc.perform(patch("/api/v1/tickets/{id}/pagar", ticketId)
                        .with(req -> { SecurityContextHolder.getContext().setAuthentication(empleadoAuth()); return req; }))
                .andExpect(status().isNotFound());
    }

    // ---- PATCH /api/v1/tickets/{id}/anular ----

    @Test
    void anular_retorna200() throws Exception {
        UUID ticketId = UUID.randomUUID();
        AnularTicketRequest request = new AnularTicketRequest();
        request.setMotivo("Error de registro");

        when(ticketService.anular(eq(ticketId), any(), any()))
                .thenReturn(sampleResponse(ticketId, EstadoTicket.ANULADO));

        mockMvc.perform(patch("/api/v1/tickets/{id}/anular", ticketId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(request))
                        .with(req -> { SecurityContextHolder.getContext().setAuthentication(empleadoAuth()); return req; }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estadoTicket").value("ANULADO"));
    }

    // ---- GET /api/v1/tickets/{id} ----

    @Test
    void obtenerPorId_retorna200() throws Exception {
        UUID ticketId = UUID.randomUUID();
        when(ticketService.obtenerPorId(ticketId)).thenReturn(sampleResponse(ticketId, EstadoTicket.ACTIVO));

        mockMvc.perform(get("/api/v1/tickets/{id}", ticketId)
                        .with(req -> { SecurityContextHolder.getContext().setAuthentication(empleadoAuth()); return req; }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codigo").value("TKT-000001"));
    }

    @Test
    void obtenerPorId_noExiste_lanzaException() throws Exception {
        UUID ticketId = UUID.randomUUID();
        when(ticketService.obtenerPorId(ticketId))
                .thenThrow(new RecursoNoEncontradoException("no existe"));

        mockMvc.perform(get("/api/v1/tickets/{id}", ticketId)
                        .with(req -> { SecurityContextHolder.getContext().setAuthentication(empleadoAuth()); return req; }))
                .andExpect(status().isNotFound());
    }

    // ---- GET /api/v1/tickets/codigo/{codigo} ----

    @Test
    void obtenerPorCodigo_retorna200() throws Exception {
        when(ticketService.obtenerPorCodigo("TKT-000001"))
                .thenReturn(sampleResponse(UUID.randomUUID(), EstadoTicket.PAGADO));

        mockMvc.perform(get("/api/v1/tickets/codigo/{codigo}", "TKT-000001")
                        .with(req -> { SecurityContextHolder.getContext().setAuthentication(empleadoAuth()); return req; }))
                .andExpect(status().isOk());
    }

    // ---- GET /api/v1/tickets ----

    @Test
    void listar_sinFiltro_retorna200() throws Exception {
        Page<TicketResponse> page = new PageImpl<>(List.of(sampleResponse(UUID.randomUUID(), EstadoTicket.ACTIVO)));
        when(ticketService.listar(any(), any())).thenReturn(page);

        mockMvc.perform(get("/api/v1/tickets")
                        .with(req -> { SecurityContextHolder.getContext().setAuthentication(empleadoAuth()); return req; }))
                .andExpect(status().isOk());
    }

    @Test
    void listar_conFiltroEstado_retorna200() throws Exception {
        Page<TicketResponse> page = new PageImpl<>(List.of(sampleResponse(UUID.randomUUID(), EstadoTicket.PAGADO)));
        when(ticketService.listar(eq(EstadoTicket.PAGADO), any())).thenReturn(page);

        mockMvc.perform(get("/api/v1/tickets")
                        .param("estado", "PAGADO")
                        .with(req -> { SecurityContextHolder.getContext().setAuthentication(empleadoAuth()); return req; }))
                .andExpect(status().isOk());
    }

    // ---- GET /api/v1/tickets/activo/espacio/{idEspacio} ----

    @Test
    void obtenerActivoPorEspacio_retorna200() throws Exception {
        UUID espacioId = UUID.randomUUID();
        when(ticketService.obtenerActivoPorEspacio(espacioId))
                .thenReturn(sampleResponse(UUID.randomUUID(), EstadoTicket.ACTIVO));

        mockMvc.perform(get("/api/v1/tickets/activo/espacio/{idEspacio}", espacioId)
                        .with(req -> { SecurityContextHolder.getContext().setAuthentication(empleadoAuth()); return req; }))
                .andExpect(status().isOk());
    }

    @Test
    void obtenerActivoPorEspacio_noActivo_lanzaException() throws Exception {
        UUID espacioId = UUID.randomUUID();
        when(ticketService.obtenerActivoPorEspacio(espacioId))
                .thenThrow(new RecursoNoEncontradoException("sin ticket activo"));

        mockMvc.perform(get("/api/v1/tickets/activo/espacio/{idEspacio}", espacioId)
                        .with(req -> { SecurityContextHolder.getContext().setAuthentication(empleadoAuth()); return req; }))
                .andExpect(status().isNotFound());
    }
}
