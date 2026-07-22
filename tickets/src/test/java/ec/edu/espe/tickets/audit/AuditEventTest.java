package ec.edu.espe.tickets.audit;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AuditEventTest {

    @Test
    void builder_creaEventoConTodosLosCampos() {
        Object datos = "snapshot";
        AuditEvent event = AuditEvent.builder()
                .servicio("ms-tickets")
                .accion("CREATE")
                .entidad("TICKET")
                .datos(datos)
                .usuario("empleado1")
                .rol("RECAUDADOR")
                .ip("192.168.1.1")
                .mac("AA:BB:CC:DD:EE:FF")
                .build();

        assertEquals("ms-tickets", event.getServicio());
        assertEquals("CREATE", event.getAccion());
        assertEquals("TICKET", event.getEntidad());
        assertSame(datos, event.getDatos());
        assertEquals("empleado1", event.getUsuario());
        assertEquals("RECAUDADOR", event.getRol());
        assertEquals("192.168.1.1", event.getIp());
        assertEquals("AA:BB:CC:DD:EE:FF", event.getMac());
    }

    @Test
    void builder_conCamposNulos() {
        AuditEvent event = AuditEvent.builder()
                .servicio("ms-tickets")
                .accion("UPDATE")
                .entidad("TICKET")
                .datos(null)
                .usuario(null)
                .rol(null)
                .ip(null)
                .mac(null)
                .build();

        assertNull(event.getUsuario());
        assertNull(event.getRol());
        assertNull(event.getIp());
        assertNull(event.getMac());
    }
}
