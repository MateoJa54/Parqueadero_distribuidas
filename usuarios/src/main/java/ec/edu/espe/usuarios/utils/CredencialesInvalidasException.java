package ec.edu.espe.usuarios.utils;

/**
 * Se lanza cuando el username no existe o la contrasena no coincide.
 * Se mapea a HTTP 401 para no revelar cual de los dos fallo.
 */
public class CredencialesInvalidasException extends RuntimeException {

    public CredencialesInvalidasException(String mensaje) {
        super(mensaje);
    }
}
