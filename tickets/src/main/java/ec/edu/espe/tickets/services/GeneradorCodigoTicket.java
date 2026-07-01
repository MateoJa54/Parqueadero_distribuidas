package ec.edu.espe.tickets.services;

import org.springframework.stereotype.Service;

import ec.edu.espe.tickets.repositories.TicketRepository;
import lombok.RequiredArgsConstructor;

/**
 * Genera codigos legibles y unicos para los tickets con formato {@code TKT-000001}.
 * Parte del total de tickets existentes y avanza hasta encontrar un codigo libre,
 * evitando colisiones ante borrados logicos o concurrencia.
 */
@Service
@RequiredArgsConstructor
public class GeneradorCodigoTicket {

    private static final String PREFIJO = "TKT-";
    private static final String FORMATO = PREFIJO + "%06d";

    private final TicketRepository ticketRepository;

    public String generar() {
        long siguiente = ticketRepository.count() + 1;
        String codigo = String.format(FORMATO, siguiente);
        while (ticketRepository.existsByCodigo(codigo)) {
            siguiente++;
            codigo = String.format(FORMATO, siguiente);
        }
        return codigo;
    }
}
