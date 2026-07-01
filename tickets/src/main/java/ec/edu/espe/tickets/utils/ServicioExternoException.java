package ec.edu.espe.tickets.utils;

/**
 * Se lanza cuando un microservicio dependiente (vehiculos, asignaciones o
 * zonas) no responde o falla. Se traduce a HTTP 502/503.
 */
public class ServicioExternoException extends RuntimeException {

    public ServicioExternoException(String mensaje) {
        super(mensaje);
    }

    public ServicioExternoException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
