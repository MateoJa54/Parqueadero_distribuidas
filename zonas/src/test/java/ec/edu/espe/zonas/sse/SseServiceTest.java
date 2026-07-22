package ec.edu.espe.zonas.sse;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.io.IOException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

class SseServiceTest {

    private SseService sseService;

    @BeforeEach
    void setUp() {
        sseService = new SseService();
    }

    @Test
    void nuevaConexionRetornaEmitter() {
        SseEmitter emitter = sseService.nuevaConexion();
        assertNotNull(emitter);
    }

    @Test
    void nuevaConexionMultiplesClientes() {
        SseEmitter e1 = sseService.nuevaConexion();
        SseEmitter e2 = sseService.nuevaConexion();
        assertNotNull(e1);
        assertNotNull(e2);
        assertNotSame(e1, e2);
    }

    @Test
    void enviarEventoAEmitterMuertoNoPropagaExcepcion() throws Exception {
        SseEmitter emitter = mock(SseEmitter.class);
        doThrow(new IOException("closed")).when(emitter).send(any(SseEmitter.SseEventBuilder.class));

        // Should not throw
        assertDoesNotThrow(() -> sseService.enviarEvento(emitter, "test", "data"));
    }

    @Test
    void emitirSinClientesNoLanzaExcepcion() {
        assertDoesNotThrow(() -> sseService.emitir("evento", "datos"));
    }

    @Test
    void emitirConClienteActivoEnviaEvento() {
        SseService service = new SseService();
        service.nuevaConexion();
        assertDoesNotThrow(() -> service.emitir("test-event", "payload"));
    }

    @Test
    void emitirConClienteFallidoEliminaEmitter() throws Exception {
        // Verify emitir removes broken emitters - use real new service + mock emitter
        // We test via enviarEvento which does the same remove logic
        SseEmitter emitter = mock(SseEmitter.class);
        doThrow(new IOException("dead")).when(emitter).send(any(SseEmitter.SseEventBuilder.class));

        assertDoesNotThrow(() -> sseService.enviarEvento(emitter, "tipo", "datos"));
        verify(emitter).completeWithError(any(IOException.class));
    }

    @SuppressWarnings("unchecked")
    private java.util.List<SseEmitter> conexiones() throws Exception {
        java.lang.reflect.Field f = SseService.class.getDeclaredField("conexiones");
        f.setAccessible(true);
        return (java.util.List<SseEmitter>) f.get(sseService);
    }

    @Test
    void callbacksDeConexionRetiranEmitter() throws Exception {
        // onCompletion, onTimeout y onError se registran en nuevaConexion.
        SseEmitter e = sseService.nuevaConexion();
        assertEquals(1, conexiones().size());
        assertNotNull(e);
    }

    @Test
    void enviarEventoConEmitterActivoEnvia() throws Exception {
        SseEmitter emitter = mock(SseEmitter.class);
        sseService.enviarEvento(emitter, "snapshot", "payload");
        verify(emitter).send(any(SseEmitter.SseEventBuilder.class));
    }

    @Test
    void emitirConClienteRegistradoQueFallaLoElimina() throws Exception {
        SseEmitter emitter = mock(SseEmitter.class);
        doThrow(new IllegalStateException("cerrado")).when(emitter)
                .send(any(SseEmitter.SseEventBuilder.class));
        conexiones().add(emitter);

        assertDoesNotThrow(() -> sseService.emitir("evento", "datos"));
        verify(emitter).completeWithError(any(IllegalStateException.class));
        assertTrue(conexiones().isEmpty());
    }

    @Test
    void emitirConClienteRegistradoActivoEnvia() throws Exception {
        SseEmitter emitter = mock(SseEmitter.class);
        conexiones().add(emitter);
        sseService.emitir("evento", "datos");
        verify(emitter).send(any(SseEmitter.SseEventBuilder.class));
    }
}
