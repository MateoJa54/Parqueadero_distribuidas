package ec.edu.espe.usuarios.utils;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * Utilidad para hashear y verificar contraseñas con BCrypt.
 *
 * BCrypt genera internamente un <strong>salt</strong> aleatorio por cada hash y
 * lo embebe en el propio resultado (formato {@code $2a$10$<salt+hash>}, 60
 * caracteres). Esto significa que dos usuarios con la misma contraseña producen
 * hashes distintos, y que no es posible revertir el hash a texto plano.
 *
 * La columna {@code password_hash} se define como {@code varchar(60)} para
 * almacenar el hash completo sin truncarlo.
 */
public final class PasswordUtil {

    private static final BCryptPasswordEncoder ENCODER = new BCryptPasswordEncoder();

    private PasswordUtil() {
    }

    /**
     * Genera un hash BCrypt (con salt aleatorio) de la contraseña en texto plano.
     * @param raw contraseña en texto plano
     * @return hash BCrypt de 60 caracteres
     */
    public static String hash(String raw) {
        return ENCODER.encode(raw);
    }

    /**
     * Verifica si una contraseña en texto plano coincide con un hash BCrypt.
     * @param raw  contraseña en texto plano
     * @param hash hash BCrypt previamente almacenado
     * @return true si coinciden
     */
    public static boolean matches(String raw, String hash) {
        return ENCODER.matches(raw, hash);
    }
}
