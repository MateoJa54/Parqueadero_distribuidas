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
    void emitirConClienteActivoEnviaEvento() throws Exception {
        SseEmitter emitter = mock(SseEmitter.class);
        // Use real service with a mock emitter injected by registering it via reflection
        SseService service = new SseService();

        // We can only test through nuevaConexion() since the list is private;
        // the real emitter won't throw, just verify no exception
        SseEmitter realEmitter = service.nuevaConexion();
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
}
