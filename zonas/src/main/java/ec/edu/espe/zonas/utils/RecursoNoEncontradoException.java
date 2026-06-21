package ec.edu.espe.zonas.utils;

/**
 * Se lanza cuando no existe el recurso solicitado (zona o espacio).
 * El {@link GlobalExceptionHandler} la traduce a HTTP 404 (Not Found).
 */
public class RecursoNoEncontradoException extends RuntimeException {

    public RecursoNoEncontradoException(String mensaje) {
        super(mensaje);
    }
}
