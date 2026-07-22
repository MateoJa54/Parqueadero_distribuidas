package ec.edu.espe.zonas.sse;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import ec.edu.espe.zonas.services.EspacioServicio;
import lombok.RequiredArgsConstructor;

/**
 * Canal SSE que consume el dashboard de clientes.
 *
 * <p>Es de solo lectura y publico: el navegador se suscribe con
 * {@code EventSource}, que no puede enviar cabeceras de autorizacion, y los
 * datos expuestos (estado de los espacios) no son sensibles. Por eso la ruta
 * se libera en {@code SecurityConfig} y se habilita CORS aqui.</p>
 *
 * <p>Al conectarse, el cliente recibe de inmediato un evento {@code snapshot}
 * con la foto completa de los espacios, de modo que puede pintar el tablero sin
 * ninguna otra llamada. A partir de ahi solo recibe eventos incrementales
 * ({@code espacio-creado}, {@code espacio-actualizado}).</p>
 */
@RestController
@RequestMapping("/api/v1/sse")
@CrossOrigin(origins = "${cors.allowed-origins}")
@RequiredArgsConstructor
public class SseController {

    private final SseService sseService;
    private final EspacioServicio espacioServicio;

    @GetMapping(value = "/espacios", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamEspacios() {
        SseEmitter emitter = sseService.nuevaConexion();
        // Foto inicial para que el dashboard se pinte sin depender de otro endpoint.
        sseService.enviarEvento(emitter, "snapshot", espacioServicio.obtenerEspacio());
        return emitter;
    }
}
