package ec.edu.espe.tickets.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RolesTicketsTest {

    @Test
    void constantes_tienenValoresEsperados() {
        assertEquals("ROOT", RolesTickets.ROOT);
        assertEquals("ADMIN", RolesTickets.ADMIN);
        assertEquals("RECAUDADOR", RolesTickets.RECAUDADOR);
        assertTrue(RolesTickets.PUEDE_OPERAR.contains("RECAUDADOR"));
        assertTrue(RolesTickets.PUEDE_OPERAR.contains("ADMIN"));
        assertTrue(RolesTickets.PUEDE_OPERAR.contains("ROOT"));
    }
}
