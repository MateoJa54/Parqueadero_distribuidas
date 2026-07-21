package ec.edu.espe.zonas.security;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class RolesZonasTest {

    @Test
    void constantesRolesNoSonNulas() {
        assertNotNull(RolesZonas.ROOT);
        assertNotNull(RolesZonas.ADMIN);
        assertNotNull(RolesZonas.RECAUDADOR);
    }

    @Test
    void puedeAdministrarContienAdminYRoot() {
        assertTrue(RolesZonas.PUEDE_ADMINISTRAR.contains("ADMIN"));
        assertTrue(RolesZonas.PUEDE_ADMINISTRAR.contains("ROOT"));
    }

    @Test
    void puedeCambiarEstadoContieneRecaudadorAdminYRoot() {
        assertTrue(RolesZonas.PUEDE_CAMBIAR_ESTADO.contains("RECAUDADOR"));
        assertTrue(RolesZonas.PUEDE_CAMBIAR_ESTADO.contains("ADMIN"));
        assertTrue(RolesZonas.PUEDE_CAMBIAR_ESTADO.contains("ROOT"));
    }

    @Test
    void valoresRolesSonCorrectos() {
        assertEquals("ROOT", RolesZonas.ROOT);
        assertEquals("ADMIN", RolesZonas.ADMIN);
        assertEquals("RECAUDADOR", RolesZonas.RECAUDADOR);
    }
}
