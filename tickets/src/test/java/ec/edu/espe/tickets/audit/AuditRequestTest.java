package ec.edu.espe.tickets.audit;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AuditRequestTest {

    @Test
    void record_creaConTodosLosCampos() {
        Object datos = "datos";
        AuditRequest req = new AuditRequest("CREATE", "TICKET", datos);

        assertEquals("CREATE", req.accion());
        assertEquals("TICKET", req.entidad());
        assertSame(datos, req.datos());
    }

    @Test
    void record_conDatosNulos() {
        AuditRequest req = new AuditRequest("DELETE", "TICKET", null);
        assertNull(req.datos());
    }

    @Test
    void record_equalsYHashCode() {
        AuditRequest a = new AuditRequest("CREATE", "TICKET", "datos");
        AuditRequest b = new AuditRequest("CREATE", "TICKET", "datos");
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void record_toString_contieneValores() {
        AuditRequest req = new AuditRequest("UPDATE", "TICKET", "datos");
        String s = req.toString();
        assertTrue(s.contains("UPDATE"));
        assertTrue(s.contains("TICKET"));
    }
}
