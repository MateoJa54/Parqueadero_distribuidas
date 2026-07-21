package ec.edu.espe.tickets.utils;

import ec.edu.espe.tickets.dtos.TicketResponse;
import ec.edu.espe.tickets.entities.EstadoTicket;
import ec.edu.espe.tickets.entities.Ticket;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class TicketMapperTest {

    @Test
    void aResponse_mapearTodosLosCampos() {
        UUID id = UUID.randomUUID();
        UUID idEspacio = UUID.randomUUID();
        UUID idUsuario = UUID.randomUUID();
        UUID idVehiculo = UUID.randomUUID();
        UUID idEmpleado = UUID.randomUUID();
        OffsetDateTime ingreso = OffsetDateTime.now().minusHours(2);
        OffsetDateTime salida = OffsetDateTime.now();
        OffsetDateTime createdAt = OffsetDateTime.now().minusHours(3);
        OffsetDateTime updatedAt = OffsetDateTime.now();

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
                .fechaHoraSalida(salida)
                .estadoTicket(EstadoTicket.PAGADO)
                .idEmpleado(idEmpleado)
                .valorRecaudado(new BigDecimal("3.50"))
                .motivoAnulacion(null)
                .createdAt(createdAt)
                .updatedAt(updatedAt)
                .build();

        TicketResponse response = TicketMapper.aResponse(ticket);

        assertNotNull(response);
        assertEquals(id, response.getId());
        assertEquals("TKT-000001", response.getCodigo());
        assertEquals(idEspacio, response.getIdEspacio());
        assertEquals("E-01", response.getCodigoEspacio());
        assertEquals("AUTO", response.getTipoEspacio());
        assertEquals(idUsuario, response.getIdUsuario());
        assertEquals(idVehiculo, response.getIdVehiculo());
        assertEquals("ABC-1234", response.getPlaca());
        assertEquals("Auto", response.getTipoVehiculo());
        assertEquals("CLIENTE", response.getCategoriaTarifa());
        assertEquals(ingreso, response.getFechaHoraIngreso());
        assertEquals(salida, response.getFechaHoraSalida());
        assertEquals(EstadoTicket.PAGADO, response.getEstadoTicket());
        assertEquals(idEmpleado, response.getIdEmpleado());
        assertEquals(new BigDecimal("3.50"), response.getValorRecaudado());
        assertNull(response.getMotivoAnulacion());
        assertEquals(createdAt, response.getCreatedAt());
        assertEquals(updatedAt, response.getUpdatedAt());
    }

    @Test
    void aResponse_conMotivoAnulacion() {
        Ticket ticket = Ticket.builder()
                .id(UUID.randomUUID())
                .codigo("TKT-000002")
                .idEspacio(UUID.randomUUID())
                .codigoEspacio("E-02")
                .tipoEspacio("MOTO")
                .idUsuario(UUID.randomUUID())
                .idVehiculo(UUID.randomUUID())
                .placa("XYZ-9999")
                .tipoVehiculo("Motocicleta")
                .categoriaTarifa("INVITADO")
                .fechaHoraIngreso(OffsetDateTime.now().minusMinutes(30))
                .estadoTicket(EstadoTicket.ANULADO)
                .idEmpleado(UUID.randomUUID())
                .valorRecaudado(BigDecimal.ZERO)
                .motivoAnulacion("Error de placa")
                .build();

        TicketResponse response = TicketMapper.aResponse(ticket);

        assertEquals("Error de placa", response.getMotivoAnulacion());
        assertEquals(EstadoTicket.ANULADO, response.getEstadoTicket());
    }
}
