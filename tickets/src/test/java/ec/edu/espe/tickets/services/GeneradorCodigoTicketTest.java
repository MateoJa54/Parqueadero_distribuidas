package ec.edu.espe.tickets.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;

import ec.edu.espe.tickets.repositories.TicketRepository;

class GeneradorCodigoTicketTest {

    @Test
    void generaUnCodigoLegibleConSeisDigitos() {
        TicketRepository repository = mock(TicketRepository.class);
        when(repository.siguienteNumeroCodigoTicket()).thenReturn(42L);

        assertEquals("TKT-000042", new GeneradorCodigoTicket(repository).generar());
    }

    @Test
    void conservaNumerosMayoresASeisDigitos() {
        TicketRepository repository = mock(TicketRepository.class);
        when(repository.siguienteNumeroCodigoTicket()).thenReturn(1_000_000L);

        assertEquals("TKT-1000000", new GeneradorCodigoTicket(repository).generar());
    }
}
