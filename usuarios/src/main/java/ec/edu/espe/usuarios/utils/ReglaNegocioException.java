package ec.edu.espe.usuarios.utils;

/**
 * Se lanza cuando la operación viola una regla de negocio o genera un conflicto
 * (username/dni/email duplicado, persona ya con usuario, rol ya asignado, etc.).
 * El {@link GlobalExceptionHandler} la traduce a HTTP 409 (Conflict).
 */
public class ReglaNegocioException extends RuntimeException {

    public ReglaNegocioException(String mensaje) {
        super(mensaje);
    }
}
