package ec.edu.espe.tickets.services;

import org.springframework.stereotype.Service;

import ec.edu.espe.tickets.repositories.TicketRepository;
import lombok.RequiredArgsConstructor;

/**
 * Genera codigos legibles y unicos para los tickets con formato {@code TKT-000001}.
 * Usa una secuencia de la base de datos (atomica y thread-safe), por lo que dos
 * ingresos concurrentes nunca obtienen el mismo codigo.
 */
@Service
@RequiredArgsConstructor
public class GeneradorCodigoTicket {

    private static final String PREFIJO = "TKT-";
    private static final String FORMATO = PREFIJO + "%06d";

    private final TicketRepository ticketRepository;

    public String generar() {
        long siguiente = ticketRepository.siguienteNumeroCodigoTicket();
        return String.format(FORMATO, siguiente);
    }
}
