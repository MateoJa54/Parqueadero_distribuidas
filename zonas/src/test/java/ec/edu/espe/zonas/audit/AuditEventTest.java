package ec.edu.espe.zonas.audit;

import static org.junit.jupiter.api.Assertions.*;

import java.util.UUID;

import org.junit.jupiter.api.Test;

class AuditEventTest {

    @Test
    void builderCreaEventoConTodosLosCampos() {
        String userId = UUID.randomUUID().toString();
        AuditEvent event = AuditEvent.builder()
                .servicio("ms-zonas")
                .accion("CREATE")
                .entidad("ZONA")
                .datos("datos-objeto")
                .usuario(userId)
                .rol("ADMIN")
                .ip("192.168.1.1")
                .mac("AA:BB:CC:DD:EE:FF")
                .build();

        assertEquals("ms-zonas", event.getServicio());
        assertEquals("CREATE", event.getAccion());
        assertEquals("ZONA", event.getEntidad());
        assertEquals("datos-objeto", event.getDatos());
        assertEquals(userId, event.getUsuario());
        assertEquals("ADMIN", event.getRol());
        assertEquals("192.168.1.1", event.getIp());
        assertEquals("AA:BB:CC:DD:EE:FF", event.getMac());
    }

    @Test
    void builderCreaEventoConCamposNulos() {
        AuditEvent event = AuditEvent.builder()
                .servicio("ms-zonas")
                .accion("UPDATE")
                .entidad("ESPACIO")
                .datos(null)
                .usuario(null)
                .rol(null)
                .ip(null)
                .mac(null)
                .build();

        assertNotNull(event);
        assertEquals("ms-zonas", event.getServicio());
        assertNull(event.getUsuario());
        assertNull(event.getDatos());
    }

    @Test
    void dosEventosConMismosDatossonIguales() {
        AuditEvent e1 = AuditEvent.builder()
                .servicio("ms-zonas").accion("DELETE").entidad("ZONA")
                .datos("d").usuario("u").rol("r").ip("1.1.1.1").mac("m").build();
        AuditEvent e2 = AuditEvent.builder()
                .servicio("ms-zonas").accion("DELETE").entidad("ZONA")
                .datos("d").usuario("u").rol("r").ip("1.1.1.1").mac("m").build();

        assertEquals(e1, e2);
        assertEquals(e1.hashCode(), e2.hashCode());
    }
}
