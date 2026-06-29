package ec.edu.espe.usuarios.security;

import java.util.List;

/**
 * Nombres de los roles BASE del sistema.
 *
 * No es un enum a proposito: los roles viven como filas en la tabla {@code roles}
 * y el administrador puede crear roles adicionales en caliente sin recompilar.
 * Estas constantes solo sirven para:
 *   1. Sembrar los roles base al arranque ({@code RolesSeeder}).
 *   2. Referenciar el rol que se asigna por defecto al registrarse ({@code CLIENTE}).
 *   3. Declarar de forma legible las reglas {@code @PreAuthorize} de los endpoints.
 *
 * La autorizacion es DINAMICA: el JWT transporta los nombres de rol que el
 * usuario tenga activos (sean base o creados despues), y Spring los convierte en
 * authorities {@code ROLE_<nombre>}.
 */
public final class RolesBase {

    /** Super usuario: acceso total. */
    public static final String ROOT = "ROOT";
    /** Gestion de zonas, espacios, vehiculos, roles-usuarios y asignaciones. */
    public static final String ADMIN = "ADMIN";
    /** Reservado para el cobro de tickets (sin permisos por ahora). */
    public static final String RECAUDADOR = "RECAUDADOR";
    /** Usuario final: sus datos, sus vehiculos y la lectura del catalogo. */
    public static final String CLIENTE = "CLIENTE";
    /** Acceso publico minimo. Un peticion sin token se trata como invitado. */
    public static final String INVITADO = "INVITADO";

    /** Roles que se siembran automaticamente si no existen. */
    public static final List<String> BASE = List.of(ROOT, ADMIN, RECAUDADOR, CLIENTE, INVITADO);

    private RolesBase() {
    }
}
