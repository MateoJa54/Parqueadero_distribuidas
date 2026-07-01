package ec.edu.espe.tickets.utils;

import ec.edu.espe.tickets.dtos.TicketResponse;
import ec.edu.espe.tickets.entities.Ticket;

/** Traduccion entre la entidad {@link Ticket} y su vista publica {@link TicketResponse}. */
public final class TicketMapper {

    private TicketMapper() {
    }

    public static TicketResponse aResponse(Ticket ticket) {
        return TicketResponse.builder()
                .id(ticket.getId())
                .codigo(ticket.getCodigo())
                .idEspacio(ticket.getIdEspacio())
                .codigoEspacio(ticket.getCodigoEspacio())
                .tipoEspacio(ticket.getTipoEspacio())
                .idUsuario(ticket.getIdUsuario())
                .idVehiculo(ticket.getIdVehiculo())
                .placa(ticket.getPlaca())
                .tipoVehiculo(ticket.getTipoVehiculo())
                .fechaHoraIngreso(ticket.getFechaHoraIngreso())
                .fechaHoraSalida(ticket.getFechaHoraSalida())
                .estadoTicket(ticket.getEstadoTicket())
                .idEmpleado(ticket.getIdEmpleado())
                .valorRecaudado(ticket.getValorRecaudado())
                .motivoAnulacion(ticket.getMotivoAnulacion())
                .createdAt(ticket.getCreatedAt())
                .updatedAt(ticket.getUpdatedAt())
                .build();
    }
}
