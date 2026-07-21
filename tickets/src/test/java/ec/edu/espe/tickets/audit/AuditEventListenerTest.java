package ec.edu.espe.tickets.audit;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuditEventListenerTest {

    @Test
    void alConfirmar_delegaAPublisher() {
        AuditPublisher publisher = mock(AuditPublisher.class);
        AuditEventListener listener = new AuditEventListener(publisher);

        Object datos = "datos-test";
        AuditRequest evento = new AuditRequest("CREATE", "TICKET", datos);

        listener.alConfirmar(evento);

        verify(publisher).publicar("CREATE", "TICKET", datos);
    }

    @Test
    void alConfirmar_conDatosNulos_noPropagaExcepcion() {
        AuditPublisher publisher = mock(AuditPublisher.class);
        AuditEventListener listener = new AuditEventListener(publisher);

        AuditRequest evento = new AuditRequest("DELETE", "TICKET", null);

        assertDoesNotThrow(() -> listener.alConfirmar(evento));
        verify(publisher).publicar("DELETE", "TICKET", null);
    }
}
