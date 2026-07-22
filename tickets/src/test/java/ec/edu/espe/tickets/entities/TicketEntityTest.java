package ec.edu.espe.tickets.entities;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class TicketEntityTest {

    @Test
    void builder_creaEntidadConTodosLosCampos() {
        UUID id = UUID.randomUUID();
        UUID idEspacio = UUID.randomUUID();
        UUID idUsuario = UUID.randomUUID();
        UUID idVehiculo = UUID.randomUUID();
        UUID idEmpleado = UUID.randomUUID();
        OffsetDateTime ingreso = OffsetDateTime.now();

        Ticket ticket = Ticket.builder()
                .id(id)
                .codigo("TKT-000001")
                .idEspacio(idEspacio)
                .codigoEspacio("E-01")
                .tipoEspacio("AUTO")
                .idUsuario(idUsuario)
                .idVehiculo(idVehiculo)
                .placa("ABC-1234")
                .tipoVehiculo("Auto")
                .categoriaTarifa("CLIENTE")
                .fechaHoraIngreso(ingreso)
                .estadoTicket(EstadoTicket.ACTIVO)
                .idEmpleado(idEmpleado)
                .valorRecaudado(BigDecimal.ZERO)
                .build();

        assertEquals(id, ticket.getId());
        assertEquals("TKT-000001", ticket.getCodigo());
        assertEquals(idEspacio, ticket.getIdEspacio());
        assertEquals("E-01", ticket.getCodigoEspacio());
        assertEquals("AUTO", ticket.getTipoEspacio());
        assertEquals(idUsuario, ticket.getIdUsuario());
        assertEquals(idVehiculo, ticket.getIdVehiculo());
        assertEquals("ABC-1234", ticket.getPlaca());
        assertEquals("Auto", ticket.getTipoVehiculo());
        assertEquals("CLIENTE", ticket.getCategoriaTarifa());
        assertEquals(ingreso, ticket.getFechaHoraIngreso());
        assertEquals(EstadoTicket.ACTIVO, ticket.getEstadoTicket());
        assertEquals(idEmpleado, ticket.getIdEmpleado());
        assertEquals(BigDecimal.ZERO, ticket.getValorRecaudado());
    }

    @Test
    void noArgsConstructor_creaEntidadVacia() {
        Ticket ticket = new Ticket();
        assertNull(ticket.getId());
        assertNull(ticket.getCodigo());
        assertNull(ticket.getEstadoTicket());
    }

    @Test
    void setter_modificaEstado() {
        Ticket ticket = new Ticket();
        ticket.setEstadoTicket(EstadoTicket.PAGADO);
        ticket.setValorRecaudado(new BigDecimal("5.00"));
        ticket.setMotivoAnulacion("test");

        assertEquals(EstadoTicket.PAGADO, ticket.getEstadoTicket());
        assertEquals(new BigDecimal("5.00"), ticket.getValorRecaudado());
        assertEquals("test", ticket.getMotivoAnulacion());
    }

    @Test
    void estadoTicket_valoresEnum() {
        assertEquals(3, EstadoTicket.values().length);
        assertNotNull(EstadoTicket.valueOf("ACTIVO"));
        assertNotNull(EstadoTicket.valueOf("PAGADO"));
        assertNotNull(EstadoTicket.valueOf("ANULADO"));
    }
}
