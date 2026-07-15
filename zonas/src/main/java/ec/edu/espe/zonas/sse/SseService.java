package ec.edu.espe.zonas.sse;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Servicio de eventos en tiempo real (Server-Sent Events) para el monitoreo de
 * espacios.
 *
 * <p>Es el equivalente en Spring MVC al {@code Subject} de RxJS que usa NestJS:
 * mantiene una lista de suscriptores ({@link SseEmitter}) y difunde a todos el
 * mismo evento cuando un espacio se crea o cambia de estado. La comunicacion es
 * unidireccional (servidor -> navegador); el cliente solo observa, nunca
 * responde.</p>
 *
 * <p>La lista es {@link CopyOnWriteArrayList} porque se lee (difusion) desde el
 * hilo que procesa la peticion HTTP que cambia un espacio y se modifica
 * (alta/baja de conexiones) desde los callbacks del contenedor; esa estructura
 * evita {@code ConcurrentModificationException} sin bloqueos explicitos.</p>
 */
@Service
public class SseService {

    private static final Logger log = LoggerFactory.getLogger(SseService.class);

    /** 30 minutos; pasado ese tiempo el navegador reconecta automaticamente. */
    private static final long TIMEOUT_MS = 30L * 60L * 1000L;

    private final List<SseEmitter> conexiones = new CopyOnWriteArrayList<>();

    /**
     * Registra un nuevo cliente y devuelve su emisor. Los callbacks retiran la
     * conexion de la lista cuando el navegador se desconecta, expira o falla.
     */
    public SseEmitter nuevaConexion() {
        SseEmitter emitter = new SseEmitter(TIMEOUT_MS);

        emitter.onCompletion(() -> {
            conexiones.remove(emitter);
            log.info("Conexion SSE finalizada. Activas: {}", conexiones.size());
        });
        emitter.onTimeout(() -> {
            emitter.complete();
            conexiones.remove(emitter);
        });
        emitter.onError(e -> conexiones.remove(emitter));

        conexiones.add(emitter);
        log.info("Nueva conexion SSE. Activas: {}", conexiones.size());
        return emitter;
    }

    /**
     * Envia un evento a UNA sola conexion (se usa para el snapshot inicial que
     * recibe el cliente al conectarse).
     */
    public void enviarEvento(SseEmitter emitter, String tipo, Object data) {
        try {
            emitter.send(SseEmitter.event().name(tipo).data(data));
        } catch (IOException ex) {
            conexiones.remove(emitter);
            emitter.completeWithError(ex);
        }
    }

    /**
     * Difunde un evento a TODOS los clientes conectados. Las conexiones muertas
     * se descartan silenciosamente.
     */
    public void emitir(String tipo, Object data) {
        log.info("Emitiendo evento SSE '{}' a {} cliente(s)", tipo, conexiones.size());
        for (SseEmitter emitter : conexiones) {
            try {
                emitter.send(SseEmitter.event().name(tipo).data(data));
            } catch (IOException | IllegalStateException ex) {
                conexiones.remove(emitter);
                emitter.completeWithError(ex);
            }
        }
    }
}
