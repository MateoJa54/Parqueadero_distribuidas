package ec.edu.espe.tickets.utils;

/** Se lanza cuando un recurso solicitado no existe. Se traduce a HTTP 404. */
public class RecursoNoEncontradoException extends RuntimeException {

    public RecursoNoEncontradoException(String mensaje) {
        super(mensaje);
    }
}
