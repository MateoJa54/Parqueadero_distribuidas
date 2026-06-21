package ec.edu.espe.usuarios.utils;

/**
 * Se lanza cuando no existe el recurso solicitado (persona, usuario o rol).
 * El {@link GlobalExceptionHandler} la traduce a HTTP 404 (Not Found).
 */
public class RecursoNoEncontradoException extends RuntimeException {

    public RecursoNoEncontradoException(String mensaje) {
        super(mensaje);
    }
}
