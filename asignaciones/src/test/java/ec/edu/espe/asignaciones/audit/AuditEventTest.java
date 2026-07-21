package ec.edu.espe.asignaciones.audit;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AuditEventTest {

    @Test
    void builderAndGetters() {
        AuditEvent event = AuditEvent.builder()
                .servicio("ms-asignaciones")
                .accion("CREATE")
                .entidad("ASIGNACION")
                .datos("some data")
                .usuario("admin")
                .rol("ADMIN")
                .ip("192.168.1.1")
                .mac("AA:BB:CC:DD:EE:FF")
                .build();

        assertEquals("ms-asignaciones", event.getServicio());
        assertEquals("CREATE", event.getAccion());
        assertEquals("ASIGNACION", event.getEntidad());
        assertEquals("some data", event.getDatos());
        assertEquals("admin", event.getUsuario());
        assertEquals("ADMIN", event.getRol());
        assertEquals("192.168.1.1", event.getIp());
        assertEquals("AA:BB:CC:DD:EE:FF", event.getMac());
    }

    @Test
    void equalsAndHashCode() {
        AuditEvent e1 = AuditEvent.builder().servicio("s").accion("a").entidad("e").datos("d").usuario("u").rol("r").ip("127.0.0.1").mac("00:00:00:00:00:00").build();
        AuditEvent e2 = AuditEvent.builder().servicio("s").accion("a").entidad("e").datos("d").usuario("u").rol("r").ip("127.0.0.1").mac("00:00:00:00:00:00").build();
        assertEquals(e1, e2);
        assertEquals(e1.hashCode(), e2.hashCode());
    }
}
