package ec.edu.espe.zonas.utils;

/**
 * Se lanza cuando la operación viola una regla de negocio o genera un conflicto
 * (nombre/código duplicado, capacidad excedida, estado incompatible, etc.).
 * El {@link GlobalExceptionHandler} la traduce a HTTP 409 (Conflict).
 */
public class ReglaNegocioException extends RuntimeException {

    public ReglaNegocioException(String mensaje) {
        super(mensaje);
    }
}
