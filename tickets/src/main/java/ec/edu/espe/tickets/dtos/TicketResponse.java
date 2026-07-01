package ec.edu.espe.tickets.dtos;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import ec.edu.espe.tickets.entities.EstadoTicket;
import lombok.Builder;
import lombok.Data;

/** Vista publica de un ticket. */
@Data
@Builder
public class TicketResponse {

    private UUID id;
    private String codigo;

    private UUID idEspacio;
    private String codigoEspacio;
    private String tipoEspacio;

    private UUID idUsuario;
    private UUID idVehiculo;
    private String placa;
    private String tipoVehiculo;

    private OffsetDateTime fechaHoraIngreso;
    private OffsetDateTime fechaHoraSalida;

    private EstadoTicket estadoTicket;
    private UUID idEmpleado;
    private BigDecimal valorRecaudado;
    private String motivoAnulacion;

    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
