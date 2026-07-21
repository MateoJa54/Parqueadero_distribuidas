package ec.edu.espe.usuarios.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RolesBaseTest {

    @Test
    void constantesYListaBase() {
        assertEquals("ROOT", RolesBase.ROOT);
        assertEquals("ADMIN", RolesBase.ADMIN);
        assertEquals("RECAUDADOR", RolesBase.RECAUDADOR);
        assertEquals("CLIENTE", RolesBase.CLIENTE);
        assertEquals("INVITADO", RolesBase.INVITADO);
        assertEquals(5, RolesBase.BASE.size());
        assertTrue(RolesBase.BASE.contains(RolesBase.ROOT));
    }
}
