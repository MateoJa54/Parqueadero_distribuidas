package ec.edu.espe.tickets.utils;

/** Se lanza cuando se viola una regla de negocio. Se traduce a HTTP 409. */
public class ReglaNegocioException extends RuntimeException {

    public ReglaNegocioException(String mensaje) {
        super(mensaje);
    }
}
